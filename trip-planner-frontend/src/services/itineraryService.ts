import { api } from "./api";
import type {
  ItineraryResponse, ActivityResponse, CreateActivityRequest,
  UpdateActivityRequest,
} from "../types/trip";

export async function getItinerary(tripId: number): Promise<ItineraryResponse> {
  const { data } = await api.get<ItineraryResponse>(`/trips/${tripId}/itinerary`);
  return data;
}

export async function addActivity(tripId: number, request: CreateActivityRequest): Promise<ActivityResponse> {
  const { data } = await api.post<ActivityResponse>(`/trips/${tripId}/activities`, request);
  return data;
}

export async function updateActivity(activityId: number, request: UpdateActivityRequest): Promise<ActivityResponse> {
  const { data } = await api.put<ActivityResponse>(`/activities/${activityId}`, request);
  return data;
}

export async function deleteActivity(activityId: number): Promise<void> {
  await api.delete(`/activities/${activityId}`);
}

export async function reorderActivity(activityId: number, newOrder: number): Promise<ActivityResponse> {
  const { data } = await api.put<ActivityResponse>(`/activities/${activityId}/reorder`, { newOrder });
  return data;
}
