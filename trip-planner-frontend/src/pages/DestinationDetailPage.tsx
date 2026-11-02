import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { MapPin, ArrowLeft } from "lucide-react";
import Button from "../components/ui/Button";
import { LoadingState, ErrorState } from "../components/ui/States";
import { getDestination } from "../services/exploreService";
import { getErrorMessage } from "../services/api";
import type { DestinationResponse } from "../types/explore";

function imageUrl(seed: string, width: number, height: number) {
  // Same Lorem Picsum placeholder approach as DestinationCard - see that
  // file's comment for why, and the README for what to swap it for.
  return `https://picsum.photos/seed/${seed}/${width}/${height}`;
}

export default function DestinationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [destination, setDestination] = useState<DestinationResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    fetchDestination();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function fetchDestination() {
    setIsLoading(true);
    setError(null);
    try {
      setDestination(await getDestination(Number(id)));
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }

  if (isLoading) return <LoadingState message="Loading destination..." />;
  if (error || !destination) return <ErrorState message={error ?? "Destination not found"} onRetry={fetchDestination} />;

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-10">
      <Link to="/explore" className="flex items-center gap-1.5 text-sm text-ink-soft hover:text-primary w-fit">
        <ArrowLeft size={14} />
        Back to Explore
      </Link>

      <div className="relative rounded-[28px] overflow-hidden aspect-[16/9] mt-6">
        <img
          src={imageUrl(`waypoint-destination-${destination.id}`, 1200, 675)}
          alt={destination.name}
          className="absolute inset-0 w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-black/5 to-transparent" />
        <div className="absolute bottom-6 left-6">
          {destination.category && (
            <span className="bg-white/95 rounded-full px-3 py-1 text-xs font-semibold text-ink">
              {destination.category}
            </span>
          )}
          <h1 className="font-display text-3xl sm:text-4xl font-bold text-white mt-3">{destination.name}</h1>
          <p className="text-white/90 mt-1">{destination.country}</p>
        </div>
      </div>

      {destination.description && (
        <p className="text-ink-soft leading-relaxed mt-6 max-w-2xl">{destination.description}</p>
      )}

      <p className="flex items-center gap-1.5 text-xs text-ink-soft mt-4 font-mono">
        <MapPin size={12} />
        {destination.latitude.toFixed(4)}, {destination.longitude.toFixed(4)}
      </p>

      <Link to={`/trips/new?destination=${encodeURIComponent(destination.name)}`} className="inline-block mt-8">
        <Button size="lg">Plan a trip to {destination.name}</Button>
      </Link>
    </div>
  );
}
