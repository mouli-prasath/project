import { api } from "./api";
import type { ShareLinkResponse, PublicTripResponse } from "../types/share";

export async function createShareLink(tripId: number): Promise<ShareLinkResponse> {
  const { data } = await api.post<ShareLinkResponse>(`/trips/${tripId}/share`);
  return data;
}

export async function getSharedTrip(shareToken: string): Promise<PublicTripResponse> {
  const { data } = await api.get<PublicTripResponse>(`/shared/trips/${shareToken}`);
  return data;
}
