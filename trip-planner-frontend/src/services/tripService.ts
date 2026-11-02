import { api } from "./api";
import type {
  CreateTripRequest, UpdateTripRequest, TripResponse, PagedResponse,
  ItineraryResponse, ItineraryDayResponse,
} from "../types/trip";

export async function createTrip(request: CreateTripRequest): Promise<TripResponse> {
  const { data } = await api.post<TripResponse>("/trips", request);
  return data;
}

export async function listTrips(page = 0, size = 10): Promise<PagedResponse<TripResponse>> {
  const { data } = await api.get<PagedResponse<TripResponse>>("/trips", { params: { page, size } });
  return data;
}

export async function getTrip(tripId: number): Promise<TripResponse> {
  const { data } = await api.get<TripResponse>(`/trips/${tripId}`);
  return data;
}

export async function updateTrip(tripId: number, request: UpdateTripRequest): Promise<TripResponse> {
  const { data } = await api.put<TripResponse>(`/trips/${tripId}`, request);
  return data;
}

export async function deleteTrip(tripId: number): Promise<void> {
  await api.delete(`/trips/${tripId}`);
}

export async function generateItinerary(tripId: number): Promise<ItineraryResponse> {
  const { data } = await api.post<ItineraryResponse>(`/trips/${tripId}/generate`);
  return data;
}

export async function regenerateItinerary(tripId: number): Promise<ItineraryResponse> {
  const { data } = await api.post<ItineraryResponse>(`/trips/${tripId}/regenerate`);
  return data;
}

export async function regenerateDay(tripId: number, dayNumber: number): Promise<ItineraryDayResponse> {
  const { data } = await api.post<ItineraryDayResponse>(`/trips/${tripId}/days/${dayNumber}/regenerate`);
  return data;
}
