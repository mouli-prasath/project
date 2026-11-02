import { Link } from "react-router-dom";
import { Calendar, Users } from "lucide-react";
import type { TripResponse, TripStatus } from "../../types/trip";

const STATUS_STYLES: Record<TripStatus, string> = {
  DRAFT: "bg-warning-soft text-warning",
  GENERATED: "bg-success-soft text-success",
  FAILED: "bg-error-soft text-error",
  COMPLETED: "bg-teal-soft text-teal",
};

const STATUS_LABELS: Record<TripStatus, string> = {
  DRAFT: "Draft",
  GENERATED: "Ready",
  FAILED: "Generation failed",
  COMPLETED: "Completed",
};

function imageUrl(seed: string | number, width: number, height: number) {
  return `https://picsum.photos/seed/waypoint-trip-${seed}/${width}/${height}`;
}

function formatDateRange(start: string, end: string): string {
  const opts: Intl.DateTimeFormatOptions = { month: "short", day: "numeric" };
  const startDate = new Date(start).toLocaleDateString("en-US", opts);
  const endDate = new Date(end).toLocaleDateString("en-US", opts);
  return `${startDate} - ${endDate}`;
}

export default function TripCard({ trip }: { trip: TripResponse }) {
  return (
    <Link to={`/trips/${trip.id}`} className="group block">
      <div className="relative aspect-[4/3] rounded-[var(--radius-card)] overflow-hidden">
        <img
          src={imageUrl(trip.id, 400, 300)}
          alt={trip.destination}
          loading="lazy"
          className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
        />
        <span className={`absolute top-3 left-3 rounded-full px-2.5 py-1 text-xs font-semibold ${STATUS_STYLES[trip.status]}`}>
          {STATUS_LABELS[trip.status]}
        </span>
      </div>
      <p className="font-display font-semibold text-ink mt-3 truncate">{trip.title || trip.destination}</p>
      <div className="flex items-center gap-3 text-sm text-ink-soft mt-1">
        <span className="flex items-center gap-1">
          <Calendar size={13} />
          {formatDateRange(trip.startDate, trip.endDate)}
        </span>
        <span className="flex items-center gap-1">
          <Users size={13} />
          {trip.travelers}
        </span>
      </div>
    </Link>
  );
}
