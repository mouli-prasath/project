import type { ItineraryDayResponse } from "./trip";

export interface ShareLinkResponse {
  shareToken: string;
  shareUrl: string;
}

export interface PublicTripResponse {
  title: string;
  destination: string;
  startDate: string;
  endDate: string;
  travelers: number;
  budget: number;
  days: ItineraryDayResponse[];
}
