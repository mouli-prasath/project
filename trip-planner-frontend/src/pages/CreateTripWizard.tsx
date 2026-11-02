import { useState } from "react";
import type { FormEvent } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { ChevronLeft, ChevronRight } from "lucide-react";
import Input from "../components/ui/Input";
import Button from "../components/ui/Button";
import { createTrip } from "../services/tripService";
import { getErrorMessage } from "../services/api";
import type { CreateTripRequest, TravelStyle } from "../types/trip";

// Matches OsmMapsClient.INTEREST_TAG_MAP on the backend exactly. Any
// interest NOT in this list is silently ignored by itinerary generation
// (Overpass has no free-text search - see backend README), so the form
// only offers interests the AI can actually act on.
const INTERESTS = [
  "BEACHES", "FOOD", "NIGHTLIFE", "HISTORY", "CULTURE",
  "ADVENTURE", "NATURE", "SHOPPING", "PHOTOGRAPHY",
];

const TRAVEL_STYLES: { value: TravelStyle; label: string }[] = [
  { value: "RELAXED", label: "Relaxed" },
  { value: "BALANCED", label: "Balanced" },
  { value: "ADVENTURE", label: "Adventure" },
  { value: "LUXURY", label: "Luxury" },
  { value: "BUDGET", label: "Budget" },
];

const STEPS = ["Destination", "Preferences", "Budget", "Review"];

export default function CreateTripWizard() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [step, setStep] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [source, setSource] = useState("");
  const [destination, setDestination] = useState(searchParams.get("destination") ?? "");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [travelers, setTravelers] = useState(2);

  const [interests, setInterests] = useState<string[]>([]);
  const [travelStyle, setTravelStyle] = useState<TravelStyle>("BALANCED");
  const [additionalPreferences, setAdditionalPreferences] = useState("");

  const [budget, setBudget] = useState(25000);

  function toggleInterest(interest: string) {
    setInterests((prev) =>
      prev.includes(interest) ? prev.filter((i) => i !== interest) : [...prev, interest]
    );
  }

  function validateStep(currentStep: number): string | null {
    if (currentStep === 0) {
      if (!source.trim() || !destination.trim()) return "Source and destination are required";
      if (!startDate || !endDate) return "Pick your travel dates";
      if (new Date(endDate) < new Date(startDate)) return "End date can't be before start date";
      if (travelers < 1) return "At least 1 traveler is required";
    }
    if (currentStep === 2) {
      if (budget <= 0) return "Budget must be greater than 0";
    }
    return null;
  }

  function goNext() {
    const validationError = validateStep(step);
    if (validationError) {
      setError(validationError);
      return;
    }
    setError(null);
    setStep((s) => Math.min(s + 1, STEPS.length - 1));
  }

  function goBack() {
    setError(null);
    setStep((s) => Math.max(s - 1, 0));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    const request: CreateTripRequest = {
      source, destination, startDate, endDate, travelers, budget, travelStyle,
      interests, additionalPreferences: additionalPreferences || undefined,
    };

    try {
      const trip = await createTrip(request);
      navigate(`/trips/${trip.id}/generating`);
    } catch (err) {
      setError(getErrorMessage(err));
      setIsSubmitting(false);
    }
  }

  return (
    <div className="max-w-2xl mx-auto px-4 sm:px-6 py-10">
      <h1 className="font-display text-3xl font-bold text-ink">Plan your trip</h1>

      {/* Progress */}
      <div className="flex items-center gap-2 mt-6 mb-10">
        {STEPS.map((label, i) => (
          <div key={label} className="flex items-center gap-2 flex-1">
            <div
              className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0 ${
                i <= step ? "bg-primary text-white" : "bg-bg-alt text-ink-soft"
              }`}
            >
              {i + 1}
            </div>
            <span className={`text-xs font-semibold hidden sm:block ${i <= step ? "text-ink" : "text-ink-soft"}`}>
              {label}
            </span>
            {i < STEPS.length - 1 && <div className={`h-0.5 flex-1 ${i < step ? "bg-primary" : "bg-bg-alt"}`} />}
          </div>
        ))}
      </div>

      <form onSubmit={handleSubmit}>
        {step === 0 && (
          <div className="flex flex-col gap-4">
            <Input label="Where are you starting from?" value={source} onChange={(e) => setSource(e.target.value)} placeholder="e.g. Chennai" />
            <Input label="Where do you want to go?" value={destination} onChange={(e) => setDestination(e.target.value)} placeholder="e.g. Goa" />
            <div className="grid grid-cols-2 gap-4">
              <Input label="Start date" type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
              <Input label="End date" type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
            </div>
            <Input label="Travelers" type="number" min={1} max={20} value={travelers} onChange={(e) => setTravelers(Number(e.target.value))} />
          </div>
        )}

        {step === 1 && (
          <div className="flex flex-col gap-6">
            <div>
              <p className="text-sm font-semibold text-ink mb-3">What are you interested in?</p>
              <div className="flex flex-wrap gap-2">
                {INTERESTS.map((interest) => (
                  <button
                    key={interest}
                    type="button"
                    onClick={() => toggleInterest(interest)}
                    className={`px-3.5 py-1.5 rounded-full text-xs font-semibold capitalize transition-colors ${
                      interests.includes(interest) ? "bg-primary text-white" : "bg-bg-alt text-ink hover:bg-border"
                    }`}
                  >
                    {interest.toLowerCase()}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <p className="text-sm font-semibold text-ink mb-3">Travel style</p>
              <div className="flex flex-wrap gap-2">
                {TRAVEL_STYLES.map((s) => (
                  <button
                    key={s.value}
                    type="button"
                    onClick={() => setTravelStyle(s.value)}
                    className={`px-3.5 py-1.5 rounded-full text-xs font-semibold transition-colors ${
                      travelStyle === s.value ? "bg-primary text-white" : "bg-bg-alt text-ink hover:bg-border"
                    }`}
                  >
                    {s.label}
                  </button>
                ))}
              </div>
            </div>

            <Input
              label="Anything else? (optional)"
              value={additionalPreferences}
              onChange={(e) => setAdditionalPreferences(e.target.value)}
              placeholder="e.g. Prefer less crowded places"
            />
          </div>
        )}

        {step === 2 && (
          <div className="flex flex-col gap-4">
            <Input label="Total budget (₹)" type="number" min={1} value={budget} onChange={(e) => setBudget(Number(e.target.value))} />
            <p className="text-xs text-ink-soft">
              This covers your whole trip - accommodation, food, transport, and activities combined.
            </p>
          </div>
        )}

        {step === 3 && (
          <div className="flex flex-col gap-3 bg-bg-alt rounded-[var(--radius-card)] p-6">
            <div className="flex justify-between text-sm"><span className="text-ink-soft">From</span><span className="font-medium text-ink">{source}</span></div>
            <div className="flex justify-between text-sm"><span className="text-ink-soft">To</span><span className="font-medium text-ink">{destination}</span></div>
            <div className="flex justify-between text-sm"><span className="text-ink-soft">Dates</span><span className="font-medium text-ink">{startDate} to {endDate}</span></div>
            <div className="flex justify-between text-sm"><span className="text-ink-soft">Travelers</span><span className="font-medium text-ink">{travelers}</span></div>
            <div className="flex justify-between text-sm"><span className="text-ink-soft">Budget</span><span className="font-medium text-ink">₹{budget.toLocaleString()}</span></div>
            <div className="flex justify-between text-sm"><span className="text-ink-soft">Style</span><span className="font-medium text-ink capitalize">{travelStyle.toLowerCase()}</span></div>
            <div className="flex justify-between text-sm items-start"><span className="text-ink-soft">Interests</span><span className="font-medium text-ink capitalize text-right">{interests.length ? interests.map((i) => i.toLowerCase()).join(", ") : "None specified"}</span></div>
          </div>
        )}

        {error && (
          <p role="alert" className="text-sm text-error bg-error-soft rounded-2xl px-4 py-3 mt-4">
            {error}
          </p>
        )}

        <div className="flex justify-between mt-8">
          <Button type="button" variant="secondary" onClick={goBack} disabled={step === 0} icon={<ChevronLeft size={16} />}>
            Back
          </Button>
          {step < STEPS.length - 1 ? (
            <Button type="button" onClick={goNext} icon={<ChevronRight size={16} />} className="flex-row-reverse">
              Next
            </Button>
          ) : (
            <Button type="submit" isLoading={isSubmitting}>
              Generate itinerary
            </Button>
          )}
        </div>
      </form>
    </div>
  );
}
