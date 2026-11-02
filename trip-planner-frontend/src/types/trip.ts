export type TravelStyle = "RELAXED" | "BALANCED" | "ADVENTURE" | "LUXURY" | "BUDGET";
export type TripStatus = "DRAFT" | "GENERATED" | "FAILED" | "COMPLETED";

export interface CreateTripRequest {
  source: string;
  destination: string;
  startDate: string; // "YYYY-MM-DD"
  endDate: string;
  travelers: number;
  budget: number;
  travelStyle: TravelStyle;
  interests: string[];
  additionalPreferences?: string;
  preferredTransport?: string;
  accommodationPreference?: string;
  title?: string;
}

export interface UpdateTripRequest {
  title?: string;
  startDate?: string;
  endDate?: string;
  travelers?: number;
  budget?: number;
  travelStyle?: TravelStyle;
  interests?: string[];
  additionalPreferences?: string;
  preferredTransport?: string;
  accommodationPreference?: string;
}

export interface TripResponse {
  id: number;
  title: string;
  source: string;
  destination: string;
  startDate: string;
  endDate: string;
  travelers: number;
  budget: number;
  travelStyle: TravelStyle;
  status: TripStatus;
  interests: string[];
  additionalPreferences: string | null;
  preferredTransport: string | null;
  accommodationPreference: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ActivityResponse {
  id: number;
  placeId: number;
  placeName: string;
  address: string | null;
  latitude: number;
  longitude: number;
  startTime: string | null; // "HH:mm:ss"
  endTime: string | null;
  durationMinutes: number | null;
  estimatedCost: number | null;
  description: string | null;
  category: string | null;
  order: number;
  reason: string | null;
  weatherNote: string | null;
}

export interface ItineraryDayResponse {
  id: number;
  dayNumber: number;
  date: string;
  theme: string | null;
  activities: ActivityResponse[];
}

export interface ItineraryResponse {
  tripId: number;
  tripTitle: string;
  summary: string | null;
  days: ItineraryDayResponse[];
}

export interface CreateActivityRequest {
  dayNumber: number;
  placeName: string;
  address?: string;
  latitude: number;
  longitude: number;
  startTime?: string;
  endTime?: string;
  durationMinutes?: number;
  estimatedCost?: number;
  description?: string;
  category?: string;
}

export interface UpdateActivityRequest {
  startTime?: string;
  endTime?: string;
  durationMinutes?: number;
  estimatedCost?: number;
  description?: string;
  category?: string;
}

export interface ReorderActivityRequest {
  newOrder: number;
}
