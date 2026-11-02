import { useEffect, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { Sparkles, AlertCircle } from "lucide-react";
import Button from "../components/ui/Button";
import { generateItinerary, regenerateItinerary } from "../services/tripService";
import { getErrorMessage } from "../services/api";

const PROGRESS_MESSAGES = [
  "Understanding your preferences...",
  "Finding real places nearby...",
  "Planning your daily activities...",
  "Checking distances and routes...",
  "Preparing your itinerary...",
];

export default function GeneratingPage() {
  const { tripId } = useParams<{ tripId: string }>();
  const [searchParams] = useSearchParams();
  const isRegenerate = searchParams.get("mode") === "regenerate";
  const navigate = useNavigate();

  const [messageIndex, setMessageIndex] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!tripId) return;

    // Cycle through progress messages while the real API call is in
    // flight. This is cosmetic pacing, not a literal progress percentage -
    // the actual generation is a single backend call that takes 5-15s.
    const messageTimer = setInterval(() => {
      setMessageIndex((i) => Math.min(i + 1, PROGRESS_MESSAGES.length - 1));
    }, 2200);

    const call = isRegenerate ? regenerateItinerary(Number(tripId)) : generateItinerary(Number(tripId));

    call
      .then(() => {
        clearInterval(messageTimer);
        navigate(`/trips/${tripId}`);
      })
      .catch((err) => {
        clearInterval(messageTimer);
        setError(getErrorMessage(err));
      });

    return () => clearInterval(messageTimer);
  }, [tripId, isRegenerate, navigate]);

  return (
    <div className="max-w-md mx-auto px-4 py-24 text-center">
      {error ? (
        <>
          <AlertCircle className="text-error mx-auto" size={32} />
          <h1 className="font-display text-2xl font-bold text-ink mt-6">Something went wrong</h1>
          <p className="text-ink-soft mt-2">{error}</p>
          <Button className="mt-6" onClick={() => navigate(`/trips/${tripId}`)}>
            Back to trip
          </Button>
        </>
      ) : (
        <>
          <div className="w-16 h-16 rounded-full bg-primary-soft flex items-center justify-center mx-auto animate-pulse">
            <Sparkles className="text-primary" size={28} />
          </div>
          <h1 className="font-display text-2xl font-bold text-ink mt-6">
            {isRegenerate ? "Updating your itinerary" : "Creating your itinerary"}
          </h1>
          <p className="text-ink-soft mt-3 transition-opacity duration-300">
            {PROGRESS_MESSAGES[messageIndex]}
          </p>
        </>
      )}
    </div>
  );
}
