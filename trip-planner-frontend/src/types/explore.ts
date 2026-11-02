export interface DestinationResponse {
  id: number;
  name: string;
  country: string;
  description: string | null;
  category: string | null;
  latitude: number;
  longitude: number;
}
