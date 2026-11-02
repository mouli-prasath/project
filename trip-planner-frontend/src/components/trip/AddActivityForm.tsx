import { useState } from "react";
import type { FormEvent } from "react";
import { Search } from "lucide-react";
import Input from "../ui/Input";
import Button from "../ui/Button";
import { geocode } from "../../services/mapService";
import { addActivity } from "../../services/itineraryService";
import { getErrorMessage } from "../../services/api";
import type { ActivityResponse } from "../../types/trip";

export default function AddActivityForm({
  tripId,
  dayNumber,
  onAdded,
  onCancel,
}: {
  tripId: number;
  dayNumber: number;
  onAdded: (activity: ActivityResponse) => void;
  onCancel: () => void;
}) {
  const [placeName, setPlaceName] = useState("");
  const [startTime, setStartTime] = useState("");
  const [description, setDescription] = useState("");

  const [isLocating, setIsLocating] = useState(false);
  const [located, setLocated] = useState<{ lat: number; lng: number; address: string } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Real places only: we geocode the typed name via the backend's
  // OpenStreetMap integration to get real coordinates, rather than letting
  // the user type in arbitrary/invented lat/lng.
  async function handleLocate() {
    if (!placeName.trim()) return;
    setIsLocating(true);
    setError(null);
    try {
      const result = await geocode(placeName);
      setLocated({ lat: result.latitude, lng: result.longitude, address: result.formattedAddress ?? placeName });
    } catch (err) {
      setError(getErrorMessage(err));
      setLocated(null);
    } finally {
      setIsLocating(false);
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!located) {
      setError("Find the location first so we have real coordinates for it.");
      return;
    }
    setIsSubmitting(true);
    setError(null);
    try {
      const activity = await addActivity(tripId, {
        dayNumber,
        placeName,
        address: located.address,
        latitude: located.lat,
        longitude: located.lng,
        startTime: startTime || undefined,
        description: description || undefined,
      });
      onAdded(activity);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-3 bg-bg-alt rounded-[var(--radius-card)] p-5 mt-4">
      <div className="flex gap-2 items-end">
        <div className="flex-1">
          <Input
            label="Place name"
            value={placeName}
            onChange={(e) => {
              setPlaceName(e.target.value);
              setLocated(null);
            }}
            placeholder="e.g. Anjuna Flea Market"
          />
        </div>
        <Button
          type="button"
          variant="secondary"
          onClick={handleLocate}
          isLoading={isLocating}
          icon={<Search size={14} />}
        >
          Find
        </Button>
      </div>

      {located && (
        <p className="text-xs text-success bg-success-soft rounded-full px-3 py-1.5 w-fit">
          Found: {located.address}
        </p>
      )}

      <Input
        label="Time (optional)"
        type="time"
        value={startTime}
        onChange={(e) => setStartTime(e.target.value)}
      />
      <Input
        label="Notes (optional)"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        placeholder="Why you're adding this"
      />

      {error && <p className="text-sm text-error">{error}</p>}

      <div className="flex gap-2 mt-1">
        <Button type="submit" size="sm" isLoading={isSubmitting} disabled={!located}>
          Add to day
        </Button>
        <Button type="button" size="sm" variant="ghost" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </form>
  );
}
