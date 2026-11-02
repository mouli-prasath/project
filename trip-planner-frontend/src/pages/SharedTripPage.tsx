import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Calendar, Users, Wallet } from "lucide-react";
import Card from "../components/ui/Card";
import { LoadingState, ErrorState } from "../components/ui/States";
import ItineraryMap from "../components/map/ItineraryMap";
import { getSharedTrip } from "../services/sharingService";
import { getErrorMessage } from "../services/api";
import type { PublicTripResponse } from "../types/share";

export default function SharedTripPage() {
  const { shareToken } = useParams<{ shareToken: string }>();
  const [trip, setTrip] = useState<PublicTripResponse | null>(null);
  const [selectedDay, setSelectedDay] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!shareToken) return;
    fetchTrip();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [shareToken]);

  async function fetchTrip() {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getSharedTrip(shareToken!);
      setTrip(data);
      setSelectedDay(data.days[0]?.dayNumber ?? 1);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }

  if (isLoading) return <LoadingState message="Loading trip..." />;
  if (error || !trip) return <ErrorState message={error ?? "Trip not found"} onRetry={fetchTrip} />;

  const currentDay = trip.days.find((d) => d.dayNumber === selectedDay);

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-10">
      <span className="text-xs font-semibold text-primary bg-primary-soft rounded-full px-3 py-1">
        Shared itinerary - read only
      </span>
      <h1 className="font-display text-3xl font-bold text-ink mt-3">{trip.title || trip.destination}</h1>
      <div className="flex flex-wrap items-center gap-x-5 gap-y-1.5 text-sm text-ink-soft mt-2">
        <span className="flex items-center gap-1.5"><Calendar size={14} />{trip.startDate} to {trip.endDate}</span>
        <span className="flex items-center gap-1.5"><Users size={14} />{trip.travelers} travelers</span>
        <span className="flex items-center gap-1.5"><Wallet size={14} />₹{trip.budget.toLocaleString()}</span>
      </div>

      <div className="grid lg:grid-cols-[1fr_1fr] gap-8 mt-8">
        <div>
          <div className="flex flex-wrap gap-2 mb-5">
            {trip.days.map((d) => (
              <button
                key={d.dayNumber}
                onClick={() => setSelectedDay(d.dayNumber)}
                className={`px-4 py-2 rounded-full text-sm font-semibold transition-colors ${
                  selectedDay === d.dayNumber ? "bg-ink text-white" : "bg-bg-alt text-ink hover:bg-border"
                }`}
              >
                Day {d.dayNumber}
              </button>
            ))}
          </div>

          {currentDay && (
            <Card padding="lg">
              <p className="text-xs font-mono text-ink-soft uppercase">{currentDay.date}</p>
              {currentDay.theme && <h3 className="font-display text-lg font-bold text-ink mt-1">{currentDay.theme}</h3>}

              <div className="mt-2">
                {currentDay.activities
                  .slice()
                  .sort((a, b) => a.order - b.order)
                  .map((activity) => (
                    <div key={activity.id} className="py-5 border-b border-border last:border-b-0">
                      {activity.startTime && (
                        <span className="font-mono text-xs text-primary font-semibold">
                          {activity.startTime.slice(0, 5)}
                        </span>
                      )}
                      <h4 className="font-display font-semibold text-ink mt-0.5">{activity.placeName}</h4>
                      {activity.description && (
                        <p className="text-sm text-ink-soft mt-1.5 leading-relaxed">{activity.description}</p>
                      )}
                      {activity.estimatedCost != null && (
                        <span className="font-mono text-xs text-ink-soft mt-2 block">
                          ₹{activity.estimatedCost.toLocaleString()}
                        </span>
                      )}
                    </div>
                  ))}
              </div>
            </Card>
          )}
        </div>

        <div className="h-[420px] lg:h-auto lg:sticky lg:top-20">
          <ItineraryMap activities={currentDay?.activities ?? []} />
        </div>
      </div>
    </div>
  );
}
