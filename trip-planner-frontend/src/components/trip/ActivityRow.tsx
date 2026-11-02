import { useState } from "react";
import { ArrowUp, ArrowDown, Trash2, MapPin, Heart, Pencil, X, Check } from "lucide-react";
import { addFavorite } from "../../services/favoriteService";
import { updateActivity } from "../../services/itineraryService";
import { getErrorMessage } from "../../services/api";
import Input from "../ui/Input";
import Button from "../ui/Button";
import type { ActivityResponse } from "../../types/trip";

interface ActivityRowProps {
  activity: ActivityResponse;
  isFirst: boolean;
  isLast: boolean;
  onMoveUp: () => void;
  onMoveDown: () => void;
  onDelete: () => void;
  onUpdated: (updated: ActivityResponse) => void;
  isBusy: boolean;
}

export default function ActivityRow({
  activity, isFirst, isLast, onMoveUp, onMoveDown, onDelete, onUpdated, isBusy,
}: ActivityRowProps) {
  const [isSaved, setIsSaved] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

  async function handleSaveFavorite() {
    setIsSaving(true);
    try {
      await addFavorite({
        placeName: activity.placeName,
        address: activity.address ?? undefined,
        category: activity.category ?? undefined,
        latitude: activity.latitude,
        longitude: activity.longitude,
      });
      setIsSaved(true);
    } catch {
      // Non-critical action - a failed favorite save shouldn't interrupt
      // the itinerary view with an error banner. The heart just stays
      // un-filled, and the user can try again.
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <div className="flex gap-4 py-5 border-b border-border last:border-b-0">
      <div className="flex flex-col items-center gap-1 pt-1 shrink-0">
        <button
          onClick={onMoveUp}
          disabled={isFirst || isBusy}
          aria-label="Move earlier"
          className="text-ink-soft hover:text-primary disabled:opacity-30 disabled:cursor-not-allowed"
        >
          <ArrowUp size={14} />
        </button>
        <span className="font-mono text-xs text-ink-soft">{activity.order}</span>
        <button
          onClick={onMoveDown}
          disabled={isLast || isBusy}
          aria-label="Move later"
          className="text-ink-soft hover:text-primary disabled:opacity-30 disabled:cursor-not-allowed"
        >
          <ArrowDown size={14} />
        </button>
      </div>

      <div className="flex-1 min-w-0">
        <div className="flex items-start justify-between gap-3">
          <div>
            {activity.startTime && (
              <span className="font-mono text-xs text-primary font-semibold">
                {activity.startTime.slice(0, 5)}
              </span>
            )}
            <h4 className="font-display font-semibold text-ink mt-0.5">{activity.placeName}</h4>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <button
              onClick={handleSaveFavorite}
              disabled={isSaved || isSaving}
              aria-label={isSaved ? "Saved to favorites" : "Save to favorites"}
            >
              <Heart size={15} className={isSaved ? "fill-primary text-primary" : "text-ink-soft hover:text-primary"} />
            </button>
            <button
              onClick={() => setIsEditing((e) => !e)}
              aria-label={isEditing ? "Close edit" : "Edit activity"}
              className="text-ink-soft hover:text-primary"
            >
              {isEditing ? <X size={15} /> : <Pencil size={14} />}
            </button>
            <button
              onClick={onDelete}
              disabled={isBusy}
              aria-label="Remove activity"
              className="text-ink-soft hover:text-error disabled:opacity-30"
            >
              <Trash2 size={16} />
            </button>
          </div>
        </div>

        {activity.description && !isEditing && (
          <p className="text-sm text-ink-soft mt-1.5 leading-relaxed">{activity.description}</p>
        )}

        {activity.reason && (
          <p className="text-xs text-teal mt-2 italic">"{activity.reason}"</p>
        )}

        {activity.weatherNote && (
          <p className="text-xs text-warning mt-1.5">☂ {activity.weatherNote}</p>
        )}

        {!isEditing && (
          <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mt-2.5 text-xs text-ink-soft">
            {activity.address && (
              <span className="flex items-center gap-1">
                <MapPin size={11} />
                {activity.address}
              </span>
            )}
            {activity.estimatedCost != null && (
              <span className="font-mono">₹{activity.estimatedCost.toLocaleString()}</span>
            )}
            {activity.durationMinutes != null && <span>{activity.durationMinutes} min</span>}
          </div>
        )}

        {isEditing && (
          <EditActivityForm
            activity={activity}
            onCancel={() => setIsEditing(false)}
            onSaved={(updated) => {
              onUpdated(updated);
              setIsEditing(false);
            }}
          />
        )}
      </div>
    </div>
  );
}

function EditActivityForm({
  activity,
  onSaved,
  onCancel,
}: {
  activity: ActivityResponse;
  onSaved: (updated: ActivityResponse) => void;
  onCancel: () => void;
}) {
  const [startTime, setStartTime] = useState(activity.startTime?.slice(0, 5) ?? "");
  const [durationMinutes, setDurationMinutes] = useState(activity.durationMinutes ?? 60);
  const [estimatedCost, setEstimatedCost] = useState(activity.estimatedCost ?? 0);
  const [description, setDescription] = useState(activity.description ?? "");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSave() {
    setIsSubmitting(true);
    setError(null);
    try {
      const updated = await updateActivity(activity.id, {
        startTime: startTime || undefined,
        durationMinutes,
        estimatedCost,
        description: description || undefined,
      });
      onSaved(updated);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="flex flex-col gap-3 bg-bg-alt rounded-2xl p-4 mt-3">
      <div className="grid grid-cols-2 gap-3">
        <Input label="Time" type="time" value={startTime} onChange={(e) => setStartTime(e.target.value)} />
        <Input label="Duration (min)" type="number" min={0} value={durationMinutes} onChange={(e) => setDurationMinutes(Number(e.target.value))} />
      </div>
      <Input label="Estimated cost (₹)" type="number" min={0} value={estimatedCost} onChange={(e) => setEstimatedCost(Number(e.target.value))} />
      <Input label="Notes" value={description} onChange={(e) => setDescription(e.target.value)} />
      {error && <p className="text-xs text-error">{error}</p>}
      <div className="flex gap-2">
        <Button size="sm" icon={<Check size={13} />} onClick={handleSave} isLoading={isSubmitting}>
          Save
        </Button>
        <Button size="sm" variant="ghost" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </div>
  );
}
