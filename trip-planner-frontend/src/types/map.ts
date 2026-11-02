export interface GeocodeResponse {
  formattedAddress: string | null;
  latitude: number;
  longitude: number;
  placeId: string | null;
}

export interface PlaceSearchResult {
  placeId: string | null;
  name: string;
  address: string | null;
  category: string;
  rating: number | null;
  latitude: number;
  longitude: number;
  openingTime: string | null;
  closingTime: string | null;
}

export interface Waypoint {
  name: string;
  latitude: number;
  longitude: number;
}

export interface OptimizeRouteRequest {
  start: Waypoint;
  waypoints: Waypoint[];
}

export interface OptimizedWaypoint {
  sequence: number;
  name: string;
  latitude: number;
  longitude: number;
  legDistanceKm: number;
}

export interface OptimizeRouteResponse {
  route: OptimizedWaypoint[];
  totalDistanceKm: number;
}
