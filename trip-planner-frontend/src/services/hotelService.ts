import { api } from "./api";
import type { HotelSearchResponse } from "../types/hotel";

export interface HotelSearchFilters {
  radiusKm?: number;
  minRating?: number;
  maxDistanceKm?: number;
  maxPriceLevel?: string;
  amenities?: string[];
}

export async function searchHotels(tripId: number, filters: HotelSearchFilters = {}): Promise<HotelSearchResponse> {
  const { data } = await api.get<HotelSearchResponse>(`/trips/${tripId}/hotels`, {
    params: {
      radiusKm: filters.radiusKm ?? 5,
      minRating: filters.minRating,
      maxDistanceKm: filters.maxDistanceKm,
      maxPriceLevel: filters.maxPriceLevel,
      amenities: filters.amenities?.join(","),
    },
  });
  return data;
}
