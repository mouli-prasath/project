import { useEffect, useState } from "react";
import { AlertTriangle, CheckCircle2 } from "lucide-react";
import Button from "../ui/Button";
import Card from "../ui/Card";
import { LoadingState, ErrorState } from "../ui/States";
import { getBudget, optimizeBudget } from "../../services/budgetService";
import { getErrorMessage } from "../../services/api";
import type { BudgetResponse } from "../../types/budget";

function money(value: number): string {
  return `₹${value.toLocaleString()}`;
}

export default function BudgetPanel({ tripId }: { tripId: number }) {
  const [budget, setBudget] = useState<BudgetResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isOptimizing, setIsOptimizing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchBudget();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tripId]);

  async function fetchBudget() {
    setIsLoading(true);
    setError(null);
    try {
      setBudget(await getBudget(tripId));
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleOptimize() {
    setIsOptimizing(true);
    setError(null);
    try {
      // Optimizing may regenerate the itinerary with cheaper choices -
      // the budget numbers refresh, but the itinerary tab will also
      // reflect new activities next time it's viewed.
      setBudget(await optimizeBudget(tripId));
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsOptimizing(false);
    }
  }

  if (isLoading) return <LoadingState message="Calculating your budget..." />;
  if (error) return <ErrorState message={error} onRetry={fetchBudget} />;
  if (!budget) return null;

  const rows: { label: string; value: number }[] = [
    { label: "Accommodation", value: budget.accommodationCost },
    { label: "Food", value: budget.foodCost },
    { label: "Transport", value: budget.transportCost },
    { label: "Activities", value: budget.activityCost },
  ];

  return (
    <Card padding="lg">
      <div className="flex items-center justify-between">
        <h3 className="font-display text-lg font-bold text-ink">Trip budget</h3>
        {budget.exceeded ? (
          <span className="flex items-center gap-1.5 text-xs font-semibold text-error bg-error-soft rounded-full px-3 py-1">
            <AlertTriangle size={13} />
            Over budget
          </span>
        ) : (
          <span className="flex items-center gap-1.5 text-xs font-semibold text-success bg-success-soft rounded-full px-3 py-1">
            <CheckCircle2 size={13} />
            Within budget
          </span>
        )}
      </div>

      <div className="flex flex-col gap-2.5 mt-5">
        {rows.map((r) => (
          <div key={r.label} className="flex justify-between text-sm">
            <span className="text-ink-soft">{r.label}</span>
            <span className="font-mono text-ink">{money(r.value)}</span>
          </div>
        ))}
      </div>

      <div className="border-t border-border mt-4 pt-4 flex justify-between font-semibold">
        <span className="text-ink">Estimated total</span>
        <span className="font-mono text-ink">{money(budget.totalEstimatedCost)}</span>
      </div>
      <div className="flex justify-between text-sm mt-1.5">
        <span className="text-ink-soft">Your budget</span>
        <span className="font-mono text-ink-soft">{money(budget.budget)}</span>
      </div>
      <div className="flex justify-between text-sm mt-1.5">
        <span className={budget.exceeded ? "text-error" : "text-success"}>
          {budget.exceeded ? "Over by" : "Remaining"}
        </span>
        <span className={`font-mono ${budget.exceeded ? "text-error" : "text-success"}`}>
          {money(Math.abs(budget.remaining))}
        </span>
      </div>

      {budget.exceeded && (
        <Button className="mt-5 w-full" isLoading={isOptimizing} onClick={handleOptimize}>
          Optimize for budget
        </Button>
      )}
    </Card>
  );
}
