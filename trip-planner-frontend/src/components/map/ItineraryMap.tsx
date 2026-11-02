import { useEffect } from "react";
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import type { ActivityResponse } from "../../types/trip";

// Leaflet's default marker icons reference image paths that don't resolve
// correctly through Vite's bundler - this is a well-known Leaflet+bundler
// issue, not a mistake. Fix: point directly at the CDN-hosted icon images.
delete (L.Icon.Default.prototype as unknown as { _getIconUrl?: unknown })._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
});

interface ItineraryMapProps {
  activities: ActivityResponse[];
}

function FitBounds({ activities }: { activities: ActivityResponse[] }) {
  const map = useMap();

  useEffect(() => {
    if (activities.length === 0) return;
    const bounds = L.latLngBounds(activities.map((a) => [a.latitude, a.longitude]));
    map.fitBounds(bounds, { padding: [40, 40], maxZoom: 15 });
  }, [activities, map]);

  return null;
}

export default function ItineraryMap({ activities }: ItineraryMapProps) {
  const withCoords = activities.filter((a) => a.latitude != null && a.longitude != null);

  if (withCoords.length === 0) {
    return (
      <div className="w-full h-full flex items-center justify-center bg-bg-alt rounded-[var(--radius-card)] text-sm text-ink-soft">
        No mapped locations for this day yet.
      </div>
    );
  }

  const center: [number, number] = [withCoords[0].latitude, withCoords[0].longitude];

  return (
    <MapContainer
      center={center}
      zoom={13}
      scrollWheelZoom
      className="w-full h-full rounded-[var(--radius-card)]"
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <FitBounds activities={withCoords} />
      {withCoords.length > 1 && (
        <Polyline
          positions={withCoords.map((a) => [a.latitude, a.longitude])}
          pathOptions={{ color: "#ff6b57", weight: 3, opacity: 0.7, dashArray: "6 8" }}
        />
      )}
      {withCoords.map((activity, index) => (
        <Marker key={activity.id} position={[activity.latitude, activity.longitude]}>
          <Popup>
            <div className="text-sm">
              <p className="font-semibold">{index + 1}. {activity.placeName}</p>
              {activity.startTime && <p className="text-xs text-gray-600">{activity.startTime.slice(0, 5)}</p>}
              {activity.description && <p className="text-xs mt-1">{activity.description}</p>}
            </div>
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
}
