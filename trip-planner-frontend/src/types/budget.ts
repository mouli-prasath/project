export interface BudgetResponse {
  accommodationCost: number;
  foodCost: number;
  transportCost: number;
  activityCost: number;
  totalEstimatedCost: number;
  budget: number;
  remaining: number;
  exceeded: boolean;
}
