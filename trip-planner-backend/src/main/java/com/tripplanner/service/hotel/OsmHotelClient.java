package com.tripplanner.service.hotel;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripplanner.dto.hotel.HotelCandidate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The ONLY class that queries Overpass for lodging. No API key required.
 *
 * Real vs. missing data, stated plainly (see HotelCandidate javadoc for
 * per-field detail): name, address, coordinates, phone, website, and
 * self-declared star category (when mapped) come from real OSM data.
 * Photos, user ratings/review counts, and pricing are simply not part of
 * OpenStreetMap's dataset - those fields are always null here, by design,
 * not by bug. See README "Known limitations" for the honest tradeoff of
 * choosing a free, keyless provider over a commercial one.
 */
@Service
public class OsmHotelClient {

    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";

    private static final List<String> LODGING_TAG_FILTERS = List.of(
            "[\"tourism\"=\"hotel\"]",
            "[\"tourism\"=\"guest_house\"]",
            "[\"tourism\"=\"hostel\"]",
            "[\"tourism\"=\"motel\"]"
    );

    private final RestClient restClient;
    private final String userAgent;

    public OsmHotelClient(RestClient mapsRestClient, @Value("${app.maps.user-agent}") String userAgent) {
        this.restClient = mapsRestClient;
        this.userAgent = userAgent;
    }

    public List<HotelCandidate> searchNearbyHotels(double latitude, double longitude, int radiusMeters, int maxResults) {
        StringBuilder query = new StringBuilder("[out:json][timeout:25];(");
        for (String filter : LODGING_TAG_FILTERS) {
            query.append("node").append(filter).append("(around:").append(radiusMeters)
                    .append(",").append(latitude).append(",").append(longitude).append(");");
            query.append("way").append(filter).append("(around:").append(radiusMeters)
                    .append(",").append(latitude).append(",").append(longitude).append(");");
        }
        query.append(");out center ").append(maxResults * 2).append(";");

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
            // Same reasoning as OsmMapsClient - Overpass's public instance
            // has no uptime guarantee. Return empty rather than surfacing a
            // raw network exception; HotelService/controller treats an
            // empty list as "no hotels found," not an error.
            return List.of();
        }

        if (root == null || !root.path("elements").isArray()) return List.of();

        Map<String, HotelCandidate> byId = new LinkedHashMap<>();
        for (JsonNode el : root.path("elements")) {
            JsonNode tags = el.path("tags");
            String name = tags.path("name").asText(null);
            if (name == null || name.isBlank()) continue;

            double lat = el.has("lat") ? el.path("lat").asDouble() : el.path("center").path("lat").asDouble();
            double lon = el.has("lon") ? el.path("lon").asDouble() : el.path("center").path("lon").asDouble();

            String type = el.path("type").asText("node");
            String osmId = el.path("id").asText("");
            String id = type + "/" + osmId;

            Double stars = parseStars(tags.path("stars").asText(null));
            String address = buildAddress(tags);
            List<String> amenities = buildAmenities(tags);
            String website = tags.path("website").asText(tags.path("contact:website").asText(null));
            // OSM's own object page - always available as a "details" link
            // even when the venue has no official website.
            String osmUrl = "https://www.openstreetmap.org/" + type + "/" + osmId;

            byId.putIfAbsent(id, new HotelCandidate(
                    id, name, address, stars, null /* no review count in OSM */,
                    null /* no pricing data in OSM */, null /* no photos in OSM */,
                    lat, lon, website, osmUrl, amenities
            ));

            if (byId.size() >= maxResults) break;
        }

        return new ArrayList<>(byId.values());
    }

    private Double parseStars(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            // OSM's "stars" tag is sometimes "4", sometimes "4S" (superior) -
            // take the leading numeric portion, discard the rest.
            String numeric = raw.replaceAll("[^0-9.]", "");
            return numeric.isBlank() ? null : Double.parseDouble(numeric);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String buildAddress(JsonNode tags) {
        List<String> parts = new ArrayList<>();
        addIfPresent(tags, "addr:housenumber", parts);
        addIfPresent(tags, "addr:street", parts);
        addIfPresent(tags, "addr:city", parts);
        if (!parts.isEmpty()) return String.join(", ", parts);
        return tags.path("addr:full").asText(null);
    }

    private void addIfPresent(JsonNode tags, String key, List<String> parts) {
        String value = tags.path(key).asText(null);
        if (value != null && !value.isBlank()) parts.add(value);
    }

    private List<String> buildAmenities(JsonNode tags) {
        List<String> amenities = new ArrayList<>();
        if (tags.has("internet_access") && !"no".equals(tags.path("internet_access").asText())) {
            amenities.add("wifi");
        }
        if ("yes".equals(tags.path("wheelchair").asText(null))) {
            amenities.add("wheelchair accessible");
        }
        if (tags.has("air_conditioning") && "yes".equals(tags.path("air_conditioning").asText())) {
            amenities.add("air conditioning");
        }
        if (tags.has("swimming_pool") || "yes".equals(tags.path("leisure:swimming_pool").asText(null))) {
            amenities.add("pool");
        }
        if ("yes".equals(tags.path("smoking").asText(null))) {
            amenities.add("smoking allowed");
        } else if ("no".equals(tags.path("smoking").asText(null))) {
            amenities.add("non-smoking");
        }
        return amenities;
    }
}
