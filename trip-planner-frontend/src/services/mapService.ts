import { api } from "./api";
import type { GeocodeResponse, OptimizeRouteRequest, OptimizeRouteResponse, PlaceSearchResult } from "../types/map";

export async function geocode(address: string): Promise<GeocodeResponse> {
  const { data } = await api.get<GeocodeResponse>("/map/geocode", { params: { address } });
  return data;
}

export async function searchPlaces(
  latitude: number, longitude: number, radiusKm = 5, category?: string, maxResults = 10
): Promise<PlaceSearchResult[]> {
  const { data } = await api.get<PlaceSearchResult[]>("/map/places/search", {
    params: { latitude, longitude, radiusKm, category, maxResults },
  });
  return data;
}

export async function optimizeRoute(request: OptimizeRouteRequest): Promise<OptimizeRouteResponse> {
  const { data } = await api.post<OptimizeRouteResponse>("/map/optimize-route", request);
  return data;
}
