import { useState } from "react";
import type { FormEvent } from "react";
import Input from "../ui/Input";
import Button from "../ui/Button";
import { updateTrip } from "../../services/tripService";
import { getErrorMessage } from "../../services/api";
import type { TripResponse } from "../../types/trip";

export default function EditTripPanel({
  trip,
  onSaved,
  onCancel,
}: {
  trip: TripResponse;
  onSaved: (updated: TripResponse) => void;
  onCancel: () => void;
}) {
  const [title, setTitle] = useState(trip.title ?? "");
  const [startDate, setStartDate] = useState(trip.startDate);
  const [endDate, setEndDate] = useState(trip.endDate);
  const [travelers, setTravelers] = useState(trip.travelers);
  const [budget, setBudget] = useState(trip.budget);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      const updated = await updateTrip(trip.id, { title, startDate, endDate, travelers, budget });
      onSaved(updated);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="bg-bg-alt rounded-[var(--radius-card)] p-6 mt-4 flex flex-col gap-4">
      <Input label="Trip title" value={title} onChange={(e) => setTitle(e.target.value)} />
      <div className="grid grid-cols-2 gap-4">
        <Input label="Start date" type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
        <Input label="End date" type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
      </div>
      <div className="grid grid-cols-2 gap-4">
        <Input label="Travelers" type="number" min={1} value={travelers} onChange={(e) => setTravelers(Number(e.target.value))} />
        <Input label="Budget (₹)" type="number" min={1} value={budget} onChange={(e) => setBudget(Number(e.target.value))} />
      </div>

      {error && <p className="text-sm text-error">{error}</p>}

      <div className="flex gap-2">
        <Button type="submit" size="sm" isLoading={isSubmitting}>Save changes</Button>
        <Button type="button" size="sm" variant="ghost" onClick={onCancel}>Cancel</Button>
      </div>

      <p className="text-xs text-ink-soft">
        Note: changing dates or travelers doesn't automatically regenerate your itinerary - use "Regenerate" separately if you want the AI to account for these changes.
      </p>
    </form>
  );
}
