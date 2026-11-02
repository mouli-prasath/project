import { api } from "./api";
import type { BudgetResponse } from "../types/budget";

export async function getBudget(tripId: number): Promise<BudgetResponse> {
  const { data } = await api.get<BudgetResponse>(`/trips/${tripId}/budget`);
  return data;
}

export async function optimizeBudget(tripId: number): Promise<BudgetResponse> {
  const { data } = await api.post<BudgetResponse>(`/trips/${tripId}/budget/optimize`);
  return data;
}
