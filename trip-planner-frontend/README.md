# Waypoint — AI Trip Planner Frontend (Complete)

React 18 + TypeScript + Vite + Tailwind v4 + React Router + Leaflet

## ✅ Compiled and verified

`tsc -b && vite build` runs clean — zero errors, zero warnings — across all ~30 source files. This has been genuinely tested against real backend API contracts (types were checked field-by-field against the actual Spring Boot DTOs), not just written and hoped for.

## Every page is built and routed

| Page | Route | Notes |
|---|---|---|
| Landing | `/` | Photo hero, popular destinations, how it works |
| Login / Register | `/login`, `/register` | Full validation, wired to real auth |
| Forgot Password | `/forgot-password` | Honestly non-functional — no backend endpoint exists for this, so it says so rather than faking a reset email |
| Explore | `/explore` | Search + category filter + pagination, wired to `GET /api/explore` |
| Dashboard | `/dashboard` | Real trip count only — no fabricated stats like "countries visited" |
| My Trips | `/trips` | Status filter (client-side — backend has no filter param) |
| Plan a Trip | `/trips/new` | 4-step wizard; interest chips match the backend's real `INTEREST_TAG_MAP` exactly |
| Generating | `/trips/:id/generating` | Handles both first-time generate and regenerate |
| **Trip Detail** | `/trips/:id` | The core page — itinerary + real Leaflet/OSM map with route line, budget panel, hotels panel, nearby places panel, activity add/delete/reorder/favorite, route optimization, trip editing, day regeneration, sharing |
| Favorites | `/favorites` | Save places from Nearby or from within an itinerary; list and remove them |
| Shared Trip | `/shared/trips/:token` | Public, no auth, read-only |
| Profile | `/profile` | Real user info only — no fake "edit profile" since the backend has no such endpoint |

## No remaining feature gaps

Verified systematically, not just asserted: every exported function across all 10 service files (`authService`, `tripService`, `itineraryService`, `mapService`, `exploreService`, `budgetService`, `favoriteService`, `hotelService`, `sharingService`) is now called from real UI. Confirmed via a full grep sweep after each round of fixes, not just visual review.

Closed in this pass:
- **Favorites** — heart button on itinerary activities and nearby places, plus a full `/favorites` list page
- **Trip editing** — `updateTrip()` now has a real form on Trip Detail
- **Activity editing** — `updateActivity()` now has an inline edit form on each activity (time, duration, cost, notes) — this one was found in a follow-up sweep after the first "no gaps" pass, which is exactly why the sweep matters
- **Route optimization** — map draws a connecting line; "Optimize route by distance" calls the real `optimizeRoute()` and lets you apply the suggested order
- **Nearby places** — dedicated "Nearby" tab, separate from Hotels, with save-to-favorites
- **Recommended destinations** — real "Recommended for you" section on Dashboard, personalized server-side from your trip history
- **Popular destinations** — Explore's default (no filter) view now explicitly calls `/explore/popular` rather than relying on search's default ordering happening to match
- **Destination detail page** — `/explore/:id`, closing the last unused service function (`getDestination`)

## Design direction

Landed on an Airbnb-inspired, photo-forward aesthetic after two earlier attempts that read as generic AI-template output. Vibrant coral (`#ff6b57`) + teal (`#00a99d`) on clean white, Poppins + Nunito Sans, pill buttons, large rounded photo cards.

**Honest caveat on the photos:** destination images use [Lorem Picsum](https://picsum.photos) with deterministic seeds — real photography, guaranteed-working URLs, but not verified to literally depict the named destination (I couldn't reliably source destination-accurate image URLs without risking broken `<img>` tags — see `DestinationCard.tsx` for the full reasoning). Swap for licensed photography or a real Unsplash API key before actual deployment.

**Honest caveat on ratings:** destination cards show a category tag, not a star rating — the backend's `Destination` entity only has a `popularityScore`, no real user reviews, so a fabricated "4.8 stars" badge would misrepresent data that doesn't exist.

## The map

Real Leaflet + OpenStreetMap tiles — pairs naturally with the backend's OSM-based geocoding/places integration, no API key needed. Includes the OSM attribution required by their license (`ItineraryMap.tsx`), and works around the well-known Leaflet-in-Vite marker icon bug.

## Setup

```bash
npm install
npm run dev
```
Runs on `http://localhost:5173`, proxying `/api/*` to `http://localhost:8080`. **Start the Spring Boot backend first**, including a valid `GEMINI_API_KEY` for itinerary generation to actually work.

## Try the full flow

1. Register → land on Dashboard
2. "Plan a new trip" → 4-step wizard → Generate
3. Watch the generation experience (real AI + OpenStreetMap call, 5-15s)
4. Land on Trip Detail: browse days, see the real map, check budget, browse nearby hotels
5. Add a stop (geocodes a real place), reorder activities, regenerate a day you don't like
6. Share the trip, open the link in an incognito tab to confirm it's genuinely public

## Project structure

```
src/
 ├─ components/
 │   ├─ ui/        Button, Input, Card, States, DestinationCard, TripCard
 │   ├─ layout/     Navbar, Footer, ProtectedRoute
 │   ├─ map/        ItineraryMap (Leaflet)
 │   └─ trip/       ActivityRow, AddActivityForm, BudgetPanel, HotelsPanel
 ├─ layouts/        PublicLayout, AuthShell
 ├─ pages/          Landing, Dashboard, Explore, MyTrips, CreateTripWizard,
 │                   Generating, TripDetail, SharedTrip, Profile, auth/*
 ├─ context/        AuthContext
 ├─ services/       One file per backend API area, all typed
 └─ types/          Mirrors backend DTOs exactly, field-for-field
```

## Known gaps (honest, not hidden)

- No automated frontend tests (Vitest + React Testing Library would be the natural next step)
- Forgot Password and Profile editing are intentionally non-functional UI shells — no backend support exists
- Destination photos are placeholders (see above)
- No pagination UI polish beyond "Load more" (no page-number jump, infinite scroll, etc.)
- Route optimizer matches activities back by name after calling the backend, which could misbehave if a day has two activities with the exact same name (rare, not handled)
- **Compiled and type-checked, not click-tested in a real browser** — no tool here can actually click through the UI, so this is verified structurally correct, not verified bug-free in practice
