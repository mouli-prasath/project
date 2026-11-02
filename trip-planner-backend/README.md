# AI Trip Planner — Backend (Complete)

Java 17 · Spring Boot 3.3 · MySQL 8 · JWT Auth · OpenStreetMap · Gemini AI

## ⚠️ Important: this code has not been compiled in my sandbox

My sandbox can only reach a small allowlist of domains and **not Maven Central**, so I could not run `mvn compile`. I've reviewed every file for balanced braces, correct imports, and cross-referenced every internal class reference against the actual files — but you should still treat the first `mvn spring-boot:run` as the real test. If it fails, read the error carefully (usually Lombok annotation processing not enabled in your IDE, or a typo) and let me know if you get stuck.

**If you're using an IDE:** enable Lombok annotation processing (IntelliJ: Settings → Build Tools → Annotation Processors → Enable) or `@Getter`/`@Setter`/`@Builder` won't resolve while editing.

## Module status — all backend modules from the spec

| Module | Status | Endpoints |
|---|---|---|
| Authentication | ✅ | `POST /api/auth/register`, `/login`, `GET /api/auth/me` |
| Trip CRUD | ✅ | `POST/GET/PUT/DELETE /api/trips[/{id}]` (paginated list) |
| Map/Location Integration | ✅ | `GET /api/map/geocode`, `GET /api/map/places/search`, `POST /api/map/optimize-route` — via OpenStreetMap (Nominatim + Overpass), no API key |
| **AI Itinerary Generation** | ✅ | `POST /api/trips/{id}/generate` |
| Regenerate (full trip / single day) | ✅ | `POST /api/trips/{id}/regenerate`, `POST /api/trips/{id}/days/{n}/regenerate` |
| AI response validation/fallback | ✅ | Built into generation - retries once, then fails clearly (see `ItineraryValidationService`) |
| Itinerary read + activity editing | ✅ | `GET /api/trips/{id}/itinerary`, `POST /api/trips/{id}/activities`, `PUT/DELETE /api/activities/{id}`, `PUT /api/activities/{id}/reorder` |
| Explainable recommendations | ✅ | Every generated activity includes a `reason` field |
| Weather-aware itinerary | ✅ (optional) | Folded into generation - degrades silently without an API key |
| Budget planner | ✅ | `GET /api/trips/{id}/budget`, `POST /api/trips/{id}/budget/optimize` |
| Favorites | ✅ | `POST/GET /api/favorites`, `DELETE /api/favorites/{id}` |
| Sharing | ✅ | `POST /api/trips/{id}/share` (auth), `GET /api/shared/trips/{token}` (public) |
| Opening-hours validation | ⚠️ dormant | Field exists but OSM's `opening_hours` format isn't parsed (see Known Limitations) - was checked when Google supplied simple hours |
| **Explore/Discovery** | ✅ | `GET /api/explore` (search+filter+paginated), `/popular`, `/recommended` (personalizes if authenticated), `/{id}` — all public |
| **Nearby Hotel Discovery** | ✅ | `GET /api/trips/{id}/hotels` — real hotels from OpenStreetMap, filterable, with distances (no ratings/photos/pricing - see below) |
| **Swagger / OpenAPI docs** | ✅ | Interactive UI at `/swagger-ui.html`, spec at `/v3/api-docs` |

**Backend is now fully complete against the spec's explicit REST API list**, plus the Nearby Hotel Discovery feature. Remaining gaps are production-hardening, not missing features: automated tests, Flyway migrations instead of `ddl-auto=update`, rate limiting on `/generate`, and profile-management endpoints (implied by the Profile Page UI in Section 17, not in the explicit REST spec).

**Not built (frontend-side / out of backend scope):** the React app itself, the interactive map UI, and PDF export. Backend has everything a frontend needs to build these.

## Setup

### 1. Prerequisites
Java 17, Maven 3.9+, MySQL 8 running locally.

### 2. Environment variables
```bash
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password
export JWT_SECRET=$(openssl rand -base64 32)
export CORS_ALLOWED_ORIGINS=http://localhost:5173
export OSM_USER_AGENT="AiTripPlanner/1.0 (contact: your-email@example.com)"  # required by Nominatim's usage policy - no API key needed
export GEMINI_API_KEY=your_gemini_key                # https://aistudio.google.com/apikey - still required, this is the AI itinerary engine
export OPENWEATHER_API_KEY=your_openweather_key      # optional - weather-aware scheduling degrades gracefully without it
export FRONTEND_BASE_URL=http://localhost:5173       # used to build share links
```

**No Google Maps API key needed anymore.** Maps/places/hotels run on OpenStreetMap's free services (Nominatim + Overpass) - no key, no billing account, no signup. `GEMINI_API_KEY` is still required since that's the AI itinerary engine, unrelated to maps.

### 3. Run
```bash
mvn spring-boot:run
```

## Swagger / OpenAPI

Interactive API docs, auto-generated from the controller annotations - no separate spec file to keep in sync.

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Raw OpenAPI JSON:** http://localhost:8080/v3/api-docs

Endpoints are grouped by tag (Authentication, Trips, Itinerary, Map, Budget, Favorites, Sharing, Explore, Hotels) matching the controller structure.

**To test protected endpoints from the UI:**
1. Expand `POST /api/auth/login` (or `/register`), execute it, copy the `token` from the response.
2. Click the green **Authorize** button (top right), paste the token as `Bearer <token>` (include the word "Bearer"), click Authorize.
3. Every request from the UI now carries that token automatically - `persist-authorization: true` in `application.yml` keeps it filled in even if you reload the page.

Public endpoints (`/api/auth/register`, `/api/auth/login`, `/api/explore/**`, `/api/shared/trips/**`) work without authorizing first.

## Nearby Hotel Discovery

`GET /api/trips/{id}/hotels?radiusKm=5&minRating=4&maxDistanceKm=3&maxPriceLevel=PRICE_LEVEL_MODERATE&amenities=pool,parking`

Follows the flow from the feature spec exactly:

```
Generated trip -> itinerary coordinates (or destination geocode as fallback)
  -> Overpass API search (tourism=hotel/guest_house/hostel/motel)
  -> normalize (name, image, rating, address, amenities, price tier, coordinates)
  -> compute distance from itinerary center + nearest individual activity
  -> apply filters -> integrate with budget -> return to frontend for cards + map markers
```

**What's real vs. computed, explicitly:**
- Name, address, coordinates, website, and OSM's own details link - all live from OpenStreetMap, nothing AI-generated. **Rating** is OSM's self-declared "stars" tag where mapped (not a review score, and frequently absent). **User rating count, price tier, and photos are always null** - OSM simply doesn't carry commercial review/pricing/photo data the way Google Places does. This is the real tradeoff of a free, keyless provider - stated plainly rather than hidden.
- Distances (`distanceFromItineraryCenterKm`, `nearestActivityDistanceKm`) - computed locally via Haversine, not from the provider, but from real coordinates.
- `remainingBudgetPerNight` - still a real number, derived from your trip budget minus already-estimated non-accommodation costs, divided by nights. `suggestedMaxPriceLevel` and per-hotel `withinSuggestedBudget` are computed but will currently always resolve to unavailable/`null`, since OSM never populates a hotel's price tier to compare against - kept structurally (harmless, and ready if a paid pricing provider is ever swapped in) rather than ripped out.
- **No numeric nightly price is ever invented, and OSM doesn't even offer a coarse tier the way Google did.** A real booking price or price tier would require a paid hotel/travel-partner API. The response's `dataDisclaimer` field says this explicitly so the frontend can surface it to users rather than implying pricing data that doesn't exist.
- Results are **not persisted** - every call re-queries OpenStreetMap live.
- **Overpass's public instance has no uptime SLA** and can be slow or occasionally unavailable. `OsmHotelClient`/`OsmMapsClient` return an empty list rather than throwing on failure, so a flaky Overpass response degrades to "no results" instead of crashing the request.

**Test it:**
```bash
curl "$BASE/api/trips/$TRIP_ID/hotels?radiusKm=5" -H "Authorization: Bearer $TOKEN"

# with filters
curl "$BASE/api/trips/$TRIP_ID/hotels?radiusKm=5&minRating=4.0" \
  -H "Authorization: Bearer $TOKEN"
# Note: minRating filters on OSM's self-declared "stars" tag, which most
# hotels in OSM don't have set - expect fewer results than with a rating
# filter on Google-sourced data. maxPriceLevel/amenities filters still work
# but rarely match anything, for the same reason (see Known Limitations).
```
Works even before `/generate` has been called (falls back to geocoding the destination), but is most useful after, since it can then report distance-to-your-actual-planned-activities.

## End-to-end test walkthrough

```bash
BASE=http://localhost:8080

# 1. Register + save token
TOKEN=$(curl -s -X POST $BASE/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Alex Kumar","email":"alex@example.com","password":"password123"}' | \
  python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# 2. Create a trip
TRIP_ID=$(curl -s -X POST $BASE/api/trips \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{
    "source": "Chennai", "destination": "Goa",
    "startDate": "2026-09-10", "endDate": "2026-09-12",
    "travelers": 2, "budget": 25000, "travelStyle": "RELAXED",
    "interests": ["BEACHES", "FOOD", "NIGHTLIFE"],
    "additionalPreferences": "Prefer less crowded places"
  }' | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

echo "Trip ID: $TRIP_ID"

# 3. Generate the AI itinerary (calls OpenStreetMap + Gemini)
curl -X POST $BASE/api/trips/$TRIP_ID/generate -H "Authorization: Bearer $TOKEN"

# 4. View the full itinerary
curl $BASE/api/trips/$TRIP_ID/itinerary -H "Authorization: Bearer $TOKEN"

# 5. Regenerate just day 2
curl -X POST $BASE/api/trips/$TRIP_ID/days/2/regenerate -H "Authorization: Bearer $TOKEN"

# 6. Check the budget
curl $BASE/api/trips/$TRIP_ID/budget -H "Authorization: Bearer $TOKEN"

# 7. Create a share link, then view it publicly (no token needed)
SHARE_TOKEN=$(curl -s -X POST $BASE/api/trips/$TRIP_ID/share -H "Authorization: Bearer $TOKEN" | \
  python3 -c "import sys,json; print(json.load(sys.stdin)['shareToken'])")
curl $BASE/api/shared/trips/$SHARE_TOKEN

# 8. Favorite a place (any real place with coordinates)
curl -X POST $BASE/api/favorites \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"placeName":"Baga Beach","latitude":15.5553,"longitude":73.7517,"category":"Beach"}'

# 9. Browse destinations (public - no token needed)
curl "$BASE/api/explore?search=beach"
curl "$BASE/api/explore/popular"
curl "$BASE/api/explore/recommended" -H "Authorization: Bearer $TOKEN"   # personalized
curl "$BASE/api/explore/recommended"                                     # anonymous - falls back to popular
```

## Key design decisions (good interview talking points)

- **Only `OsmMapsClient`, `OsmHotelClient`, and `GeminiClient` call external APIs.** Nothing else in the codebase builds an external URL - the frontend never talks to OpenStreetMap/Gemini directly, exactly as the spec's architecture section requires.
- **AI validation retries once, then fails loudly.** `ItineraryValidationService` checks for hallucinated place names (not in the real candidate list), invalid coordinates, negative costs, and opening-hours conflicts. On failure, `AiItineraryService` sends the AI a stricter follow-up prompt listing exactly what was wrong; if that still fails, the trip is marked `FAILED` and the client gets a clear error - never a silently broken itinerary saved to the DB.
- **Route optimization is a separate, reusable concern.** `RouteOptimizationService` (nearest-neighbor) isn't currently wired into itinerary generation automatically - it's exposed as its own endpoint so the frontend can call it on demand (e.g. "optimize my route" button), or so a future version of `ItineraryService` can call it internally before persisting. Kept decoupled deliberately.
- **Weather, route optimization, and now opening-hours all degrade gracefully.** No `OPENWEATHER_API_KEY`? Generation proceeds without weather hints. No opening-hours data for a venue (currently always true under OSM)? Validation simply skips that check. Overpass slow or down? Empty result list, not a crash. Nothing about these enrichments can block the core feature.
- **Ownership checks return 404, not 403**, everywhere (trips, activities, favorites) - so a user probing sequential IDs can't even confirm another user's resource exists.
- **DB-level cascade deletes** (`@OnDelete(action = CASCADE)`) on every child→parent relationship - deleting a trip cleanly removes its preferences, itinerary days, activities, and budget row without orphaned rows or FK errors.

## Known limitations (be upfront about these if asked)

- **OpenStreetMap requires attribution.** If your frontend displays a map or OSM-sourced data, OSM's license (ODbL) requires visibly crediting "© OpenStreetMap contributors" near the map, with a link to https://www.openstreetmap.org/copyright. This is a real legal requirement of using OSM data, not a suggestion - don't skip it in the frontend.
- **Nominatim/Overpass usage policy.** Both are free public services with fair-use limits (roughly 1 request/second for Nominatim, similar informal limits for Overpass) and no uptime SLA. Fine for development and a portfolio demo; real production traffic should either self-host both (they're open source) or use a paid geocoding/POI provider. `OSM_USER_AGENT` must identify your app - Nominatim will block generic/missing User-Agents.
- **Hotel data completeness varies enormously by region.** OSM is community-mapped - a well-mapped city might show 40 hotels with full details; a less-mapped area might show 2 with just a name and pin. This is inherent to crowdsourced data, not a bug in the integration.

- **Opening-hours validation is currently dormant.** OSM's `opening_hours` tag uses a compact mini-language (e.g. `Mo-Fr 08:00-18:00; Sa 09:00-13:00`) that isn't parsed here - `PlaceSearchResult.openingTime`/`closingTime` stay null, so `ItineraryValidationService`'s hours check never fires. The validation code is still there and would work immediately if a parser for OSM's opening_hours syntax were added (a small, well-scoped library exists for this: `opening_hours.js` has no direct Java equivalent, but the format is documented and parseable).
- **Weather is same-day only**, using OpenWeatherMap's free current-weather endpoint, not a real multi-day forecast (that needs a paid tier on most providers). Demonstrates the adaptive-itinerary concept without requiring a paid key.
- **Budget category-bucketing is keyword-based**, not a fixed enum, because the AI's `category` field is free text. Reasonable for a demo; a stricter category enum enforced in the AI response schema would be a clean v2 improvement.
- **This has not been compiled/run in a real environment yet** - see the warning at the top.

## Project structure
```
src/main/java/com/tripplanner/
 ├─ config/       SecurityConfig, MapClientConfig, AiClientConfig
 ├─ security/     JwtUtil, JwtAuthFilter, UserPrincipal, CustomUserDetailsService
 ├─ entity/       User, Trip, TripPreference, Place, ItineraryDay, Activity, TripBudget, Favorite
 ├─ repository/   Spring Data JPA repositories for each entity
 ├─ dto/
 │   ├─ auth/, trip/, map/, ai/, budget/, favorite/, share/
 ├─ controller/   Auth, Trip, Itinerary, Map, Budget, Favorite, Sharing
 ├─ service/
 │   ├─ AuthService, TripService, ItineraryService, PlaceService, BudgetService, FavoriteService, SharingService
 │   ├─ ai/       GeminiClient, ItineraryValidationService, AiItineraryService
 │   ├─ map/      OsmMapsClient, RouteOptimizationService, DistanceUtil
 │   ├─ hotel/    OsmHotelClient, HotelService
 │   └─ weather/  WeatherService
 └─ exception/    GlobalExceptionHandler + custom exceptions
```

## Suggested next step

Backend is feature-complete per the spec. Next: the React frontend (Section 8-24 of the spec - landing page, auth pages, dashboard, trip creation wizard, generated itinerary page with real map, my trips, profile), wired to this API. Or, if you want to harden the backend further first: unit/integration tests (JUnit + Mockito + Testcontainers), switching `ddl-auto` to Flyway migrations, and rate-limiting the `/generate` endpoint against cost abuse.
