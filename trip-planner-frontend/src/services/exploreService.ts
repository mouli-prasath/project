import { api } from "./api";
import type { DestinationResponse } from "../types/explore";
import type { PagedResponse } from "../types/trip";

export async function searchDestinations(
  search?: string, category?: string, page = 0, size = 12
): Promise<PagedResponse<DestinationResponse>> {
  const { data } = await api.get<PagedResponse<DestinationResponse>>("/explore", {
    params: { search, category, page, size },
  });
  return data;
}

export async function getPopularDestinations(page = 0, size = 12): Promise<PagedResponse<DestinationResponse>> {
  const { data } = await api.get<PagedResponse<DestinationResponse>>("/explore/popular", { params: { page, size } });
  return data;
}

export async function getRecommendedDestinations(page = 0, size = 12): Promise<PagedResponse<DestinationResponse>> {
  const { data } = await api.get<PagedResponse<DestinationResponse>>("/explore/recommended", { params: { page, size } });
  return data;
}

export async function getDestination(id: number): Promise<DestinationResponse> {
  const { data } = await api.get<DestinationResponse>(`/explore/${id}`);
  return data;
}
