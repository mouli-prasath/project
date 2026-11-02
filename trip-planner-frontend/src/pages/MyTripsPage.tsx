import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { MapPinned } from "lucide-react";
import TripCard from "../components/ui/TripCard";
import Button from "../components/ui/Button";
import { LoadingState, ErrorState, EmptyState } from "../components/ui/States";
import { listTrips } from "../services/tripService";
import { getErrorMessage } from "../services/api";
import type { TripResponse, TripStatus } from "../types/trip";

type FilterOption = "ALL" | TripStatus;

const FILTERS: { label: string; value: FilterOption }[] = [
  { label: "All", value: "ALL" },
  { label: "Ready", value: "GENERATED" },
  { label: "Draft", value: "DRAFT" },
  { label: "Completed", value: "COMPLETED" },
];

export default function MyTripsPage() {
  const [trips, setTrips] = useState<TripResponse[]>([]);
  const [filter, setFilter] = useState<FilterOption>("ALL");
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchTrips(0, true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function fetchTrips(pageToLoad: number, replace: boolean) {
    setIsLoading(true);
    setError(null);
    try {
      // The backend has no status filter on GET /api/trips, so filtering
      // by status happens client-side below rather than pretending the
      // API supports a query param it doesn't.
      const result = await listTrips(pageToLoad, 12);
      setTrips((prev) => (replace ? result.content : [...prev, ...result.content]));
      setHasMore(!result.last);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }

  function loadMore() {
    const next = page + 1;
    setPage(next);
    fetchTrips(next, false);
  }

  const filteredTrips = filter === "ALL" ? trips : trips.filter((t) => t.status === filter);

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-10">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl font-bold text-ink">My Trips</h1>
          <p className="text-ink-soft mt-1.5">Everything you've planned, in one place.</p>
        </div>
        <Link to="/trips/new">
          <Button size="sm">Plan a trip</Button>
        </Link>
      </div>

      <div className="flex flex-wrap gap-2 mt-6">
        {FILTERS.map((f) => (
          <button
            key={f.value}
            onClick={() => setFilter(f.value)}
            className={`px-3.5 py-1.5 rounded-full text-xs font-semibold transition-colors ${
              filter === f.value ? "bg-ink text-white" : "bg-bg-alt text-ink hover:bg-border"
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      <div className="mt-8">
        {isLoading && trips.length === 0 ? (
          <LoadingState message="Loading your trips..." />
        ) : error ? (
          <ErrorState message={error} onRetry={() => fetchTrips(0, true)} />
        ) : filteredTrips.length === 0 ? (
          <EmptyState
            icon={<MapPinned size={32} />}
            title={filter === "ALL" ? "You haven't planned any trips yet" : "No trips match this filter"}
            description={filter === "ALL" ? "Start your first AI-generated itinerary." : undefined}
            action={
              filter === "ALL" ? (
                <Link to="/trips/new">
                  <Button size="sm">Plan your first trip</Button>
                </Link>
              ) : undefined
            }
          />
        ) : (
          <>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-5 sm:gap-8">
              {filteredTrips.map((trip) => (
                <TripCard key={trip.id} trip={trip} />
              ))}
            </div>
            {hasMore && filter === "ALL" && (
              <div className="flex justify-center mt-10">
                <Button variant="secondary" onClick={loadMore} isLoading={isLoading}>
                  Load more
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
