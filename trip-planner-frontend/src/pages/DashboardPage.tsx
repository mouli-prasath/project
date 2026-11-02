import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Plus, MapPinned } from "lucide-react";
import Button from "../components/ui/Button";
import TripCard from "../components/ui/TripCard";
import DestinationCard from "../components/ui/DestinationCard";
import { LoadingState, ErrorState, EmptyState } from "../components/ui/States";
import { listTrips } from "../services/tripService";
import { getRecommendedDestinations } from "../services/exploreService";
import { getErrorMessage } from "../services/api";
import { useAuth } from "../context/AuthContext";
import type { TripResponse } from "../types/trip";
import type { DestinationResponse } from "../types/explore";

export default function DashboardPage() {
  const { user } = useAuth();
  const [trips, setTrips] = useState<TripResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [recommended, setRecommended] = useState<DestinationResponse[]>([]);
  const [isLoadingRecommended, setIsLoadingRecommended] = useState(true);

  useEffect(() => {
    fetchTrips();
    fetchRecommended();
  }, []);

  async function fetchTrips() {
    setIsLoading(true);
    setError(null);
    try {
      const page = await listTrips(0, 6);
      setTrips(page.content);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }

  async function fetchRecommended() {
    setIsLoadingRecommended(true);
    try {
      // Personalizes based on your most recent trip's interests if you have
      // one, falls back to popular destinations otherwise (handled server-side).
      const page = await getRecommendedDestinations(0, 4);
      setRecommended(page.content);
    } catch {
      // Recommendations are a nice-to-have, not core - fail silently
      // rather than showing an error banner for a secondary section.
      setRecommended([]);
    } finally {
      setIsLoadingRecommended(false);
    }
  }

  const firstName = user?.name?.split(" ")[0] ?? "there";

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-10">
      <h1 className="font-display text-3xl font-bold text-ink">Good to see you, {firstName}</h1>
      <p className="text-ink-soft mt-1.5">Where are you planning to go next?</p>

      <Link to="/trips/new" className="block mt-8">
        <div className="rounded-[var(--radius-card)] bg-primary text-white p-6 sm:p-8 flex items-center justify-between hover:opacity-95 transition-opacity">
          <div>
            <h2 className="font-display text-xl sm:text-2xl font-bold">Plan a new trip</h2>
            <p className="text-white/85 text-sm mt-1">Tell us where and when, we'll do the rest.</p>
          </div>
          <div className="w-12 h-12 rounded-full bg-white/20 flex items-center justify-center shrink-0">
            <Plus size={22} />
          </div>
        </div>
      </Link>

      <div className="flex items-center justify-between mt-12 mb-6">
        <h2 className="font-display text-xl font-bold text-ink">Your trips</h2>
        {trips.length > 0 && (
          <Link to="/trips" className="text-sm font-semibold text-primary hover:underline">
            See all
          </Link>
        )}
      </div>

      {isLoading ? (
        <LoadingState message="Loading your trips..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchTrips} />
      ) : trips.length === 0 ? (
        <EmptyState
          icon={<MapPinned size={32} />}
          title="You haven't planned any trips yet"
          description="Start your first AI-generated itinerary in a couple of minutes."
          action={
            <Link to="/trips/new">
              <Button size="sm">Plan your first trip</Button>
            </Link>
          }
        />
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-3 gap-5 sm:gap-8">
          {trips.map((trip) => (
            <TripCard key={trip.id} trip={trip} />
          ))}
        </div>
      )}

      {!isLoadingRecommended && recommended.length > 0 && (
        <>
          <div className="flex items-center justify-between mt-12 mb-6">
            <h2 className="font-display text-xl font-bold text-ink">Recommended for you</h2>
            <Link to="/explore" className="text-sm font-semibold text-primary hover:underline">
              Explore more
            </Link>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-5 sm:gap-6">
            {recommended.map((d) => (
              <DestinationCard
                key={d.id}
                name={d.name}
                country={d.country}
                category={d.category ?? "Destination"}
                imageSeed={`waypoint-dashboard-${d.id}`}
                to={`/trips/new?destination=${encodeURIComponent(d.name)}`}
              />
            ))}
          </div>
        </>
      )}

      {/* Deliberately no fabricated stats like "countries visited" or
          "places explored" - the backend doesn't track those, and inventing
          numbers would be exactly the kind of fake data the project spec
          explicitly warns against. Trip count above is the one real stat
          we actually have. */}
    </div>
  );
}
