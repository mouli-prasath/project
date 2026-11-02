import { useState } from "react";
import { Route, Check } from "lucide-react";
import Button from "../ui/Button";
import { optimizeRoute } from "../../services/mapService";
import { reorderActivity } from "../../services/itineraryService";
import { getErrorMessage } from "../../services/api";
import type { ActivityResponse } from "../../types/trip";
import type { OptimizeRouteResponse } from "../../types/map";

export default function RouteOptimizer({
  activities,
  onApplied,
}: {
  activities: ActivityResponse[];
  onApplied: () => void;
}) {
  const [result, setResult] = useState<OptimizeRouteResponse | null>(null);
  const [isOptimizing, setIsOptimizing] = useState(false);
  const [isApplying, setIsApplying] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const withCoords = activities.filter((a) => a.latitude != null && a.longitude != null);

  async function handleOptimize() {
    if (withCoords.length < 3) return; // start + at least 2 waypoints needed for reordering to matter
    setIsOptimizing(true);
    setError(null);
    try {
      const [start, ...rest] = withCoords;
      const response = await optimizeRoute({
        start: { name: start.placeName, latitude: start.latitude, longitude: start.longitude },
        waypoints: rest.map((a) => ({ name: a.placeName, latitude: a.latitude, longitude: a.longitude })),
      });
      setResult(response);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsOptimizing(false);
    }
  }

  async function handleApply() {
    if (!result) return;
    setIsApplying(true);
    setError(null);
    try {
      // The optimized route excludes the fixed start point (order 1) and
      // gives the rest in visiting order - apply that as activity_order 2..N.
      const [start, ...rest] = withCoords;
      const orderedRest = result.route
        .map((w) => rest.find((a) => a.placeName === w.name))
        .filter((a): a is ActivityResponse => a !== undefined);

      for (let i = 0; i < orderedRest.length; i++) {
        await reorderActivity(orderedRest[i].id, i + 2);
      }
      void start; // start stays at position 1, nothing to do for it

      setResult(null);
      onApplied();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsApplying(false);
    }
  }

  if (withCoords.length < 3) return null;

  return (
    <div className="mt-4">
      {!result ? (
        <Button variant="ghost" size="sm" icon={<Route size={14} />} onClick={handleOptimize} isLoading={isOptimizing}>
          Optimize route by distance
        </Button>
      ) : (
        <div className="bg-teal-soft rounded-2xl p-4">
          <p className="text-sm font-semibold text-ink">
            Suggested order (saves distance, {result.totalDistanceKm.toFixed(1)} km total)
          </p>
          <ol className="text-xs text-ink-soft mt-2 flex flex-wrap gap-x-1">
            {result.route.map((w, i) => (
              <span key={w.name}>
                {i > 0 && " → "}
                {w.name}
              </span>
            ))}
          </ol>
          <div className="flex gap-2 mt-3">
            <Button size="sm" icon={<Check size={13} />} onClick={handleApply} isLoading={isApplying}>
              Apply this order
            </Button>
            <Button size="sm" variant="ghost" onClick={() => setResult(null)}>
              Dismiss
            </Button>
          </div>
        </div>
      )}
      {error && <p className="text-sm text-error mt-2">{error}</p>}
    </div>
  );
}
