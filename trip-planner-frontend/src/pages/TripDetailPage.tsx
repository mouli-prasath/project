import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Calendar, Users, Wallet, Share2, RefreshCw, Trash2, Plus, Check, Pencil } from "lucide-react";
import Button from "../components/ui/Button";
import Card from "../components/ui/Card";
import { LoadingState, ErrorState, EmptyState } from "../components/ui/States";
import ItineraryMap from "../components/map/ItineraryMap";
import ActivityRow from "../components/trip/ActivityRow";
import AddActivityForm from "../components/trip/AddActivityForm";
import BudgetPanel from "../components/trip/BudgetPanel";
import HotelsPanel from "../components/trip/HotelsPanel";
import NearbyPlacesPanel from "../components/trip/NearbyPlacesPanel";
import RouteOptimizer from "../components/trip/RouteOptimizer";
import EditTripPanel from "../components/trip/EditTripPanel";
import { getTrip, deleteTrip, regenerateDay as regenerateDayApi } from "../services/tripService";
import { getItinerary, deleteActivity, reorderActivity } from "../services/itineraryService";
import { createShareLink } from "../services/sharingService";
import { getErrorMessage } from "../services/api";
import type { TripResponse, ItineraryResponse } from "../types/trip";

type Tab = "itinerary" | "budget" | "hotels" | "nearby";

export default function TripDetailPage() {
  const { tripId } = useParams<{ tripId: string }>();
  const navigate = useNavigate();
  const id = Number(tripId);

  const [trip, setTrip] = useState<TripResponse | null>(null);
  const [itinerary, setItinerary] = useState<ItineraryResponse | null>(null);
  const [selectedDay, setSelectedDay] = useState(1);
  const [tab, setTab] = useState<Tab>("itinerary");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [showAddForm, setShowAddForm] = useState(false);
  const [showEditForm, setShowEditForm] = useState(false);
  const [busyActivityId, setBusyActivityId] = useState<number | null>(null);
  const [isRegeneratingDay, setIsRegeneratingDay] = useState(false);
  const [shareUrl, setShareUrl] = useState<string | null>(null);
  const [isSharing, setIsSharing] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    fetchAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function fetchAll() {
    setIsLoading(true);
    setError(null);
    try {
      const tripData = await getTrip(id);
      setTrip(tripData);

      if (tripData.status === "GENERATED" || tripData.status === "COMPLETED") {
        const itineraryData = await getItinerary(id);
        setItinerary(itineraryData);
        setSelectedDay(itineraryData.days[0]?.dayNumber ?? 1);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleDeleteActivity(activityId: number) {
    if (!itinerary) return;
    setBusyActivityId(activityId);
    try {
      await deleteActivity(activityId);
      setItinerary({
        ...itinerary,
        days: itinerary.days.map((d) => ({
          ...d,
          activities: d.activities.filter((a) => a.id !== activityId),
        })),
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setBusyActivityId(null);
    }
  }

  async function handleReorder(activityId: number, newOrder: number) {
    if (!itinerary) return;
    setBusyActivityId(activityId);
    try {
      await reorderActivity(activityId, newOrder);
      // Simplest correct refresh: reordering shifts every sibling's order,
      // easier to re-fetch the day's real state than hand-roll the swap.
      const refreshed = await getItinerary(id);
      setItinerary(refreshed);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setBusyActivityId(null);
    }
  }

  async function handleRegenerateDay(dayNumber: number) {
    if (!confirm(`Regenerate day ${dayNumber}? This replaces its current activities.`)) return;
    setIsRegeneratingDay(true);
    setError(null);
    try {
      const newDay = await regenerateDayApi(id, dayNumber);
      setItinerary((prev) =>
        prev ? { ...prev, days: prev.days.map((d) => (d.dayNumber === dayNumber ? newDay : d)) } : prev
      );
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsRegeneratingDay(false);
    }
  }

  async function handleShare() {
    setIsSharing(true);
    try {
      const result = await createShareLink(id);
      setShareUrl(result.shareUrl);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSharing(false);
    }
  }

  function copyShareLink() {
    if (!shareUrl) return;
    navigator.clipboard.writeText(shareUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  async function handleDeleteTrip() {
    if (!confirm("Delete this trip permanently? This can't be undone.")) return;
    try {
      await deleteTrip(id);
      navigate("/trips");
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  if (isLoading) return <LoadingState message="Loading your trip..." />;
  if (error && !trip) return <ErrorState message={error} onRetry={fetchAll} />;
  if (!trip) return null;

  const currentDay = itinerary?.days.find((d) => d.dayNumber === selectedDay);
  const notYetGenerated = trip.status === "DRAFT" || trip.status === "FAILED";

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-10">
      {/* Trip header */}
      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl font-bold text-ink">{trip.title || trip.destination}</h1>
          <div className="flex flex-wrap items-center gap-x-5 gap-y-1.5 text-sm text-ink-soft mt-2">
            <span className="flex items-center gap-1.5">
              <Calendar size={14} />
              {trip.startDate} to {trip.endDate}
            </span>
            <span className="flex items-center gap-1.5">
              <Users size={14} />
              {trip.travelers} travelers
            </span>
            <span className="flex items-center gap-1.5">
              <Wallet size={14} />₹{trip.budget.toLocaleString()}
            </span>
          </div>
        </div>

        <div className="flex flex-wrap gap-2 shrink-0">
          <Button variant="secondary" size="sm" icon={<Pencil size={14} />} onClick={() => setShowEditForm((s) => !s)}>
            Edit
          </Button>
          {trip.status === "GENERATED" && (
            <Button
              variant="secondary"
              size="sm"
              icon={<RefreshCw size={14} />}
              onClick={() => navigate(`/trips/${id}/generating?mode=regenerate`)}
            >
              Regenerate
            </Button>
          )}
          <Button variant="secondary" size="sm" icon={<Share2 size={14} />} onClick={handleShare} isLoading={isSharing}>
            Share
          </Button>
          <Button variant="danger" size="sm" icon={<Trash2 size={14} />} onClick={handleDeleteTrip}>
            Delete
          </Button>
        </div>
      </div>

      {showEditForm && (
        <EditTripPanel
          trip={trip}
          onCancel={() => setShowEditForm(false)}
          onSaved={(updated) => {
            setTrip(updated);
            setShowEditForm(false);
          }}
        />
      )}

      {shareUrl && (
        <div className="flex items-center gap-3 bg-bg-alt rounded-full px-4 py-2.5 mt-4 w-fit max-w-full">
          <span className="text-sm text-ink-soft truncate">{shareUrl}</span>
          <button
            onClick={copyShareLink}
            className="text-xs font-semibold text-primary flex items-center gap-1 shrink-0"
          >
            {copied ? <Check size={13} /> : null}
            {copied ? "Copied" : "Copy"}
          </button>
        </div>
      )}

      {error && (
        <p role="alert" className="text-sm text-error bg-error-soft rounded-2xl px-4 py-3 mt-4">
          {error}
        </p>
      )}

      {/* Not generated yet */}
      {notYetGenerated && (
        <Card padding="lg" className="mt-8 text-center">
          <h2 className="font-display text-xl font-bold text-ink">
            {trip.status === "FAILED" ? "Generation didn't finish" : "Ready to build your itinerary"}
          </h2>
          <p className="text-ink-soft mt-2 max-w-md mx-auto">
            {trip.status === "FAILED"
              ? "Something went wrong last time. Try again."
              : "We'll build a day-by-day plan from real places near your destination."}
          </p>
          <Link to={`/trips/${id}/generating`} className="inline-block mt-5">
            <Button>Generate itinerary</Button>
          </Link>
        </Card>
      )}

      {/* Generated - tabs */}
      {itinerary && (
        <>
          <div className="flex gap-2 mt-8 border-b border-border">
            {(["itinerary", "budget", "hotels", "nearby"] as Tab[]).map((t) => (
              <button
                key={t}
                onClick={() => setTab(t)}
                className={`px-4 py-3 text-sm font-semibold capitalize border-b-2 -mb-px transition-colors ${
                  tab === t ? "border-primary text-primary" : "border-transparent text-ink-soft hover:text-ink"
                }`}
              >
                {t}
              </button>
            ))}
          </div>

          {tab === "itinerary" && (
            <div className="grid lg:grid-cols-[1fr_1fr] gap-8 mt-8">
              <div>
                {itinerary.summary && <p className="text-sm text-ink-soft mb-5">{itinerary.summary}</p>}

                <div className="flex flex-wrap gap-2 mb-5">
                  {itinerary.days.map((d) => (
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
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="text-xs font-mono text-ink-soft uppercase">{currentDay.date}</p>
                        {currentDay.theme && (
                          <h3 className="font-display text-lg font-bold text-ink mt-1">{currentDay.theme}</h3>
                        )}
                      </div>
                      <Button
                        variant="ghost"
                        size="sm"
                        icon={<RefreshCw size={13} />}
                        onClick={() => handleRegenerateDay(currentDay.dayNumber)}
                        isLoading={isRegeneratingDay}
                      >
                        Regenerate day
                      </Button>
                    </div>

                    <div className="mt-2">
                      {currentDay.activities.length === 0 ? (
                        <EmptyState title="No activities yet" description="Add one below." />
                      ) : (
                        currentDay.activities
                          .slice()
                          .sort((a, b) => a.order - b.order)
                          .map((activity, i, arr) => (
                            <ActivityRow
                              key={activity.id}
                              activity={activity}
                              isFirst={i === 0}
                              isLast={i === arr.length - 1}
                              isBusy={busyActivityId === activity.id}
                              onMoveUp={() => handleReorder(activity.id, activity.order - 1)}
                              onMoveDown={() => handleReorder(activity.id, activity.order + 1)}
                              onUpdated={(updated) => {
                                if (!itinerary) return;
                                setItinerary({
                                  ...itinerary,
                                  days: itinerary.days.map((d) =>
                                    d.dayNumber !== currentDay.dayNumber
                                      ? d
                                      : { ...d, activities: d.activities.map((a) => (a.id === updated.id ? updated : a)) }
                                  ),
                                });
                              }}
                              onDelete={() => handleDeleteActivity(activity.id)}
                            />
                          ))
                      )}
                    </div>

                    {currentDay.activities.length > 0 && (
                      <RouteOptimizer activities={currentDay.activities} onApplied={fetchAll} />
                    )}

                    {showAddForm ? (
                      <AddActivityForm
                        tripId={id}
                        dayNumber={currentDay.dayNumber}
                        onCancel={() => setShowAddForm(false)}
                        onAdded={() => {
                          setShowAddForm(false);
                          fetchAll();
                        }}
                      />
                    ) : (
                      <Button
                        variant="ghost"
                        size="sm"
                        icon={<Plus size={14} />}
                        onClick={() => setShowAddForm(true)}
                        className="mt-4"
                      >
                        Add a stop
                      </Button>
                    )}
                  </Card>
                )}
              </div>

              <div className="h-[420px] lg:h-auto lg:sticky lg:top-20">
                <ItineraryMap activities={currentDay?.activities ?? []} />
              </div>
            </div>
          )}

          {tab === "budget" && (
            <div className="max-w-md mt-8">
              <BudgetPanel tripId={id} />
            </div>
          )}

          {tab === "hotels" && (
            <div className="mt-8">
              <HotelsPanel tripId={id} />
            </div>
          )}

          {tab === "nearby" && (
            <div className="mt-8">
              {currentDay && currentDay.activities.length > 0 ? (
                <NearbyPlacesPanel
                  centerLat={currentDay.activities[0].latitude}
                  centerLng={currentDay.activities[0].longitude}
                />
              ) : (
                <EmptyState
                  title="No reference point yet"
                  description="Add at least one activity to this day to search nearby places around it."
                />
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
}
