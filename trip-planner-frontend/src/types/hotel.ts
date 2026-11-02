export interface HotelResponse {
  placeId: string;
  name: string;
  address: string | null;
  rating: number | null;
  userRatingCount: number | null;
  priceLevel: string | null;
  imageUrl: string | null;
  latitude: number;
  longitude: number;
  amenities: string[];
  detailsUrl: string | null;
  distanceFromItineraryCenterKm: number;
  nearestActivityName: string | null;
  nearestActivityDistanceKm: number | null;
  withinSuggestedBudget: boolean | null;
}

export interface HotelSearchResponse {
  tripId: number;
  centerLatitude: number;
  centerLongitude: number;
  radiusKm: number;
  provider: string;
  dataDisclaimer: string;
  suggestedMaxPriceLevel: string | null;
  remainingBudgetPerNight: number | null;
  hotels: HotelResponse[];
}
