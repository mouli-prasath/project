import { useEffect, useState } from "react";
import { Star, ExternalLink, Info } from "lucide-react";
import Button from "../ui/Button";
import { LoadingState, ErrorState, EmptyState } from "../ui/States";
import { searchHotels } from "../../services/hotelService";
import { getErrorMessage } from "../../services/api";
import type { HotelSearchResponse } from "../../types/hotel";

export default function HotelsPanel({ tripId }: { tripId: number }) {
  const [result, setResult] = useState<HotelSearchResponse | null>(null);
  const [radiusKm, setRadiusKm] = useState(5);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchHotels();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tripId, radiusKm]);

  async function fetchHotels() {
    setIsLoading(true);
    setError(null);
    try {
      setResult(await searchHotels(tripId, { radiusKm }));
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3 mb-5">
        <h3 className="font-display text-lg font-bold text-ink">Nearby hotels</h3>
        <div className="flex gap-1.5">
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

      {result && (
        <p className="flex items-start gap-2 text-xs text-ink-soft bg-bg-alt rounded-2xl px-4 py-3 mb-5 leading-relaxed">
          <Info size={14} className="shrink-0 mt-0.5" />
          {result.dataDisclaimer}
        </p>
      )}

      {isLoading ? (
        <LoadingState message="Searching nearby hotels..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchHotels} />
      ) : !result || result.hotels.length === 0 ? (
        <EmptyState
          title="No hotels found nearby"
          description="Try a larger search radius, or check back after generating your itinerary."
        />
      ) : (
        <div className="grid sm:grid-cols-2 gap-4">
          {result.hotels.map((hotel) => (
            <div key={hotel.placeId} className="border border-border rounded-[var(--radius-card)] p-4">
              <div className="flex items-start justify-between gap-2">
                <h4 className="font-display font-semibold text-ink">{hotel.name}</h4>
                {hotel.rating != null && (
                  <span className="flex items-center gap-1 text-xs font-semibold text-ink shrink-0">
                    <Star size={12} className="fill-primary text-primary" />
                    {hotel.rating}
                  </span>
                )}
              </div>
              {hotel.address && <p className="text-xs text-ink-soft mt-1">{hotel.address}</p>}

              <div className="flex flex-wrap gap-1.5 mt-2.5">
                {hotel.amenities.slice(0, 3).map((a) => (
                  <span key={a} className="text-[11px] bg-teal-soft text-teal rounded-full px-2 py-0.5 capitalize">
                    {a}
                  </span>
                ))}
              </div>

              <div className="flex items-center justify-between mt-3 text-xs text-ink-soft">
                <span>{hotel.distanceFromItineraryCenterKm.toFixed(1)} km from your trip</span>
                {hotel.detailsUrl && (
                  <a
                    href={hotel.detailsUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-1 text-primary font-semibold hover:underline"
                  >
                    Details <ExternalLink size={11} />
                  </a>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {!isLoading && (
        <div className="flex justify-center mt-6">
          <Button variant="secondary" size="sm" onClick={fetchHotels} isLoading={isLoading}>
            Refresh
          </Button>
        </div>
      )}
    </div>
  );
}
