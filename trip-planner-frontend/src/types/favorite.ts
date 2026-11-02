export interface CreateFavoriteRequest {
  placeName: string;
  address?: string;
  category?: string;
  description?: string;
  latitude: number;
  longitude: number;
}

export interface FavoriteResponse {
  id: number;
  placeId: number;
  placeName: string;
  address: string | null;
  category: string | null;
  latitude: number;
  longitude: number;
  createdAt: string;
}
