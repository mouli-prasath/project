package com.tripplanner.dto.hotel;

import java.util.List;

// Raw-but-normalized shape straight from OsmHotelClient, before distance
// and budget enrichment (added later by HotelService). Internal to the
// hotel module - not returned directly to the frontend.
//
// IMPORTANT - field semantics under OpenStreetMap (no API key, but real
// data limits vs. a commercial provider like Google Places):
// - rating: OSM's self-declared "stars" tag if the mapper included one
//   (e.g. a 4-star hotel), NOT a user review score. Frequently absent.
// - userRatingCount: OSM has no review system at all - always null.
// - priceLevel: OSM has no pricing data of any kind - always null.
// - imageUrl: OSM/Overpass doesn't serve photos - always null.
public record HotelCandidate(
        String placeId,
        String name,
        String address,
        Double rating,
        Integer userRatingCount,
        String priceLevel,
        String imageUrl,
        Double latitude,
        Double longitude,
        String websiteUrl,
        String googleMapsUrl,   // repurposed as an OSM.org link, see OsmHotelClient
        List<String> amenities
) {}
