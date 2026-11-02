import { useEffect, useState } from "react";
import { Heart, MapPin } from "lucide-react";
import Button from "../ui/Button";
import { LoadingState, ErrorState, EmptyState } from "../ui/States";
import { searchPlaces } from "../../services/mapService";
import { addFavorite } from "../../services/favoriteService";
import { getErrorMessage } from "../../services/api";
import type { PlaceSearchResult } from "../../types/map";

const CATEGORIES = ["", "BEACHES", "FOOD", "NIGHTLIFE", "HISTORY", "CULTURE", "NATURE", "SHOPPING"];

export default function NearbyPlacesPanel({
  centerLat,
  centerLng,
}: {
  centerLat: number;
  centerLng: number;
}) {
  const [category, setCategory] = useState("");
  const [radiusKm, setRadiusKm] = useState(5);
  const [places, setPlaces] = useState<PlaceSearchResult[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [savedIds, setSavedIds] = useState<Set<string>>(new Set());

  useEffect(() => {
    fetchPlaces();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [category, radiusKm, centerLat, centerLng]);

  async function fetchPlaces() {
    setIsLoading(true);
    setError(null);
    try {
      const result = await searchPlaces(centerLat, centerLng, radiusKm, category || undefined, 20);
      setPlaces(result);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleSave(place: PlaceSearchResult) {
    try {
      await addFavorite({
        placeName: place.name,
        address: place.address ?? undefined,
        category: place.category,
        latitude: place.latitude,
        longitude: place.longitude,
      });
      setSavedIds((prev) => new Set(prev).add(place.placeId ?? place.name));
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  return (
    <div>
      <div className="flex flex-wrap items-center gap-3 mb-5">
        <div className="flex gap-1.5 flex-wrap">
          {CATEGORIES.map((c) => (
            <button
              key={c || "all"}
              onClick={() => setCategory(c)}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold capitalize transition-colors ${
                category === c ? "bg-ink text-white" : "bg-bg-alt text-ink hover:bg-border"
              }`}
            >
              {c ? c.toLowerCase() : "All"}
            </button>
          ))}
        </div>
        <div className="flex gap-1.5 ml-auto">
          {[5, 10, 20].map((km) => (
            <button
              key={km}
              onClick={() => setRadiusKm(km)}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-colors ${
                radiusKm === km ? "bg-ink text-white" : "bg-bg-alt text-ink hover:bg-border"
              }`}
            >
              {km} km
            </button>
          ))}
        </div>
      </div>

      {isLoading ? (
        <LoadingState message="Finding nearby places..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchPlaces} />
      ) : places.length === 0 ? (
        <EmptyState title="No places found" description="Try a different category or a larger radius." />
      ) : (
        <div className="grid sm:grid-cols-2 gap-4">
          {places.map((place) => {
            const key = place.placeId ?? place.name;
            const isSaved = savedIds.has(key);
            return (
              <div key={key} className="border border-border rounded-[var(--radius-card)] p-4">
                <div className="flex items-start justify-between gap-2">
                  <h4 className="font-display font-semibold text-ink">{place.name}</h4>
                  <button
                    onClick={() => handleSave(place)}
                    disabled={isSaved}
                    aria-label={isSaved ? "Saved" : "Save to favorites"}
                    className="shrink-0"
                  >
                    <Heart size={16} className={isSaved ? "fill-primary text-primary" : "text-ink-soft hover:text-primary"} />
                  </button>
                </div>
                <span className="text-[11px] bg-teal-soft text-teal rounded-full px-2 py-0.5 capitalize inline-block mt-2">
                  {place.category}
                </span>
                {place.address && (
                  <p className="flex items-center gap-1 text-xs text-ink-soft mt-2">
                    <MapPin size={11} />
                    {place.address}
                  </p>
                )}
              </div>
            );
          })}
        </div>
      )}

      {!isLoading && (
        <div className="flex justify-center mt-6">
          <Button variant="secondary" size="sm" onClick={fetchPlaces}>
            Refresh
          </Button>
        </div>
      )}
    </div>
  );
}
