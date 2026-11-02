package com.tripplanner.service.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripplanner.dto.map.GeocodeResponse;
import com.tripplanner.dto.map.PlaceSearchResult;
import com.tripplanner.exception.ExternalServiceException;
import com.tripplanner.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * The ONLY class that calls OpenStreetMap services directly. Uses:
 * - Nominatim for geocoding (free-text address -> coordinates)
 * - Overpass API for place/POI search (tag-based structured queries)
 *
 * Neither requires an API key. Both are free public services with usage
 * policies worth knowing about (see README "Known limitations" - rate
 * limits, required User-Agent, no guaranteed uptime/SLA). Fine for a
 * portfolio project; a production deployment at real scale would want a
 * self-hosted Nominatim/Overpass instance or a paid geocoding provider.
 */
@Service
public class OsmMapsClient {

    private static final String NOMINATIM_SEARCH_URL = "https://nominatim.openstreetmap.org/search";
    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";

    /**
     * Maps the free-text "interests" your trip form collects into Overpass
     * QL tag filter clauses. This is a best-effort, hand-maintained mapping
     * (OSM has no free-text search the way Google Places does) - unmatched
     * interests are simply ignored rather than failing the request.
     */
    private static final Map<String, List<String>> INTEREST_TAG_MAP = Map.ofEntries(
            Map.entry("BEACHES", List.of("[\"natural\"=\"beach\"]")),
            Map.entry("FOOD", List.of("[\"amenity\"=\"restaurant\"]", "[\"amenity\"=\"cafe\"]")),
            Map.entry("NIGHTLIFE", List.of("[\"amenity\"=\"bar\"]", "[\"amenity\"=\"nightclub\"]")),
            Map.entry("HISTORY", List.of("[\"tourism\"=\"museum\"]", "[\"historic\"]")),
            Map.entry("CULTURE", List.of("[\"tourism\"=\"museum\"]", "[\"amenity\"=\"theatre\"]")),
            Map.entry("ADVENTURE", List.of("[\"sport\"]", "[\"leisure\"=\"sports_centre\"]")),
            Map.entry("NATURE", List.of("[\"leisure\"=\"park\"]", "[\"natural\"=\"wood\"]")),
            Map.entry("SHOPPING", List.of("[\"shop\"]")),
            Map.entry("PHOTOGRAPHY", List.of("[\"tourism\"=\"viewpoint\"]"))
    );

    private final RestClient restClient;
    private final String userAgent;

    public OsmMapsClient(RestClient mapsRestClient, @Value("${app.maps.user-agent}") String userAgent) {
        this.restClient = mapsRestClient;
        this.userAgent = userAgent;
    }

    public GeocodeResponse geocode(String address) {
        String uri = UriComponentsBuilder.fromHttpUrl(NOMINATIM_SEARCH_URL)
                .queryParam("q", address)
                .queryParam("format", "json")
                .queryParam("limit", 1)
                .queryParam("addressdetails", 1)
                .toUriString();

        JsonNode root = execute(() -> restClient.get()
                .uri(uri)
                .header("User-Agent", userAgent) // required by Nominatim's usage policy
                .retrieve()
                .body(JsonNode.class));

        if (root == null || !root.isArray() || root.isEmpty()) {
            throw new ResourceNotFoundException("No location found for: " + address);
        }

        JsonNode first = root.get(0);
        double lat = Double.parseDouble(first.path("lat").asText());
        double lon = Double.parseDouble(first.path("lon").asText());
        String displayName = first.path("display_name").asText(null);
        String osmRef = first.path("osm_type").asText("") + "/" + first.path("osm_id").asText("");

        return new GeocodeResponse(displayName, lat, lon, osmRef);
    }

    /**
     * Runs several targeted Overpass queries (general attractions/food plus
     * each mapped user interest) and merges results into one deduplicated
     * candidate pool - the real-place data the AI itinerary generator is
     * grounded in. Mirrors the shape of the old Google-backed method so
     * callers (ItineraryService) don't need to change.
     */
    public List<PlaceSearchResult> gatherCandidatePlaces(String destination, List<String> interests) {
        GeocodeResponse center = geocode(destination);

        Set<String> tagFilters = new LinkedHashSet<>();
        tagFilters.add("[\"tourism\"=\"attraction\"]");
        tagFilters.add("[\"amenity\"=\"restaurant\"]");

        if (interests != null) {
            for (String interest : interests) {
                tagFilters.addAll(INTEREST_TAG_MAP.getOrDefault(interest.trim().toUpperCase(Locale.ROOT), List.of()));
            }
        }

        return searchByTags(center.latitude(), center.longitude(), 8000, new ArrayList<>(tagFilters), 40);
    }

    /**
     * Overpass QL search: for each tag filter clause, queries both nodes and
     * ways within radiusMeters of the center point. Results are deduplicated
     * by OSM id and unnamed elements are skipped (not useful for an
     * itinerary - a place with no name can't be shown to a user).
     */
    public List<PlaceSearchResult> searchByTags(
            double lat, double lon, int radiusMeters, List<String> tagFilters, int maxResults
    ) {
        if (tagFilters.isEmpty()) return List.of();

        StringBuilder query = new StringBuilder("[out:json][timeout:25];(");
        for (String filter : tagFilters) {
            query.append("node").append(filter).append("(around:").append(radiusMeters)
                    .append(",").append(lat).append(",").append(lon).append(");");
            query.append("way").append(filter).append("(around:").append(radiusMeters)
                    .append(",").append(lat).append(",").append(lon).append(");");
        }
        query.append(");out center ").append(maxResults * 2).append(";"); // headroom before dedup/name-filtering

        JsonNode root;
        try {
            String body = "data=" + URLEncoder.encode(query.toString(), StandardCharsets.UTF_8);
            root = restClient.post()
                    .uri(OVERPASS_URL)
                    .header("User-Agent", userAgent)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            // Overpass's public instance is best-effort (no SLA) - a slow or
            // failed query here shouldn't take down itinerary generation on
            // its own; ItineraryService already treats an empty candidate
            // pool as a clear, user-facing error rather than a crash.
            return List.of();
        }

        if (root == null || !root.path("elements").isArray()) return List.of();

        Map<String, PlaceSearchResult> byId = new LinkedHashMap<>();
        for (JsonNode el : root.path("elements")) {
            JsonNode tags = el.path("tags");
            String name = tags.path("name").asText(null);
            if (name == null || name.isBlank()) continue;

            double elLat = el.has("lat") ? el.path("lat").asDouble() : el.path("center").path("lat").asDouble();
            double elLon = el.has("lon") ? el.path("lon").asDouble() : el.path("center").path("lon").asDouble();

            String id = el.path("type").asText("") + "/" + el.path("id").asText("");
            String address = buildAddress(tags);
            String category = inferCategory(tags);

            byId.putIfAbsent(id, new PlaceSearchResult(
                    id, name, address, category, null /* OSM has no user ratings */,
                    elLat, elLon,
                    null, null // OSM's opening_hours format ("Mo-Fr 08:00-18:00;...") isn't a
                               // simple HH:mm pair - not parsed here, see README limitations
            ));

            if (byId.size() >= maxResults) break;
        }

        return new ArrayList<>(byId.values());
    }

    private String buildAddress(JsonNode tags) {
        List<String> parts = new ArrayList<>();
        addIfPresent(tags, "addr:housenumber", parts);
        addIfPresent(tags, "addr:street", parts);
        addIfPresent(tags, "addr:city", parts);
        if (parts.isEmpty()) {
            String fallback = tags.path("addr:full").asText(null);
            return fallback; // may still be null - address is genuinely unknown to OSM for this element
        }
        return String.join(", ", parts);
    }

    private void addIfPresent(JsonNode tags, String key, List<String> parts) {
        String value = tags.path(key).asText(null);
        if (value != null && !value.isBlank()) parts.add(value);
    }

    private String inferCategory(JsonNode tags) {
        if (tags.has("tourism")) return tags.path("tourism").asText();
        if (tags.has("amenity")) return tags.path("amenity").asText();
        if (tags.has("historic")) return "historic site";
        if (tags.has("natural")) return tags.path("natural").asText();
        if (tags.has("shop")) return "shop";
        return "point of interest";
    }

    /**
     * Category-based search around a point (e.g. "restaurants near these
     * coordinates"). Unlike gatherCandidatePlaces (used internally for
     * itinerary generation), this is the public building block for a
     * frontend "nearby places" panel where the user picks a category rather
     * than typing free text - Overpass has no free-text search the way
     * Google Places did, so the API contract changed here deliberately (see
     * MapController).
     */
    public List<PlaceSearchResult> searchByCategory(
            double lat, double lon, int radiusMeters, String category, int maxResults
    ) {
        List<String> filters = (category != null && !category.isBlank())
                ? INTEREST_TAG_MAP.getOrDefault(category.trim().toUpperCase(Locale.ROOT),
                        List.of("[\"tourism\"=\"attraction\"]"))
                : List.of("[\"tourism\"=\"attraction\"]");

        return searchByTags(lat, lon, radiusMeters, filters, maxResults);
    }

    private JsonNode execute(java.util.function.Supplier<JsonNode> call) {
        try {
            return call.get();
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Failed to reach OpenStreetMap Nominatim: " + e.getMessage(), e);
        }
    }
}
