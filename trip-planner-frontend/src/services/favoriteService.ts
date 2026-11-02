import { api } from "./api";
import type { CreateFavoriteRequest, FavoriteResponse } from "../types/favorite";
import type { PagedResponse } from "../types/trip";

export async function addFavorite(request: CreateFavoriteRequest): Promise<FavoriteResponse> {
  const { data } = await api.post<FavoriteResponse>("/favorites", request);
  return data;
}

export async function listFavorites(page = 0, size = 10): Promise<PagedResponse<FavoriteResponse>> {
  const { data } = await api.get<PagedResponse<FavoriteResponse>>("/favorites", { params: { page, size } });
  return data;
}

export async function removeFavorite(id: number): Promise<void> {
  await api.delete(`/favorites/${id}`);
}
