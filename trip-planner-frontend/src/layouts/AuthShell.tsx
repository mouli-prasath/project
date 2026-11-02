import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { Compass } from "lucide-react";

// Same Lorem Picsum placeholder approach as DestinationCard/LandingPage -
// see those files' comments for why, and the README for what to swap it for.
function imageUrl(seed: string, width: number, height: number) {
  return `https://picsum.photos/seed/${seed}/${width}/${height}`;
}

export default function AuthShell({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: ReactNode;
}) {
  return (
    <div className="min-h-screen grid lg:grid-cols-2 bg-bg">
      <div className="hidden lg:block relative">
        <img
          src={imageUrl("waypoint-auth-travel", 900, 1200)}
          alt="Scenic travel destination"
          className="absolute inset-0 w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/10 to-transparent" />
        <div className="relative h-full flex flex-col justify-between p-10 text-white">
          <Link to="/" className="flex items-center gap-2 font-display text-lg font-bold">
            <Compass size={22} strokeWidth={2.4} />
            Waypoint
          </Link>
          <p className="text-white/90 leading-relaxed max-w-sm">
            Every stop in your itinerary is a real place, mapped and
            distance-checked, not a guess.
          </p>
        </div>
      </div>

      <div className="flex flex-col justify-center px-6 sm:px-12 lg:px-16 py-12">
        <div className="w-full max-w-sm mx-auto">
          <Link
            to="/"
            className="lg:hidden flex items-center gap-2 font-display text-lg font-bold text-ink mb-10"
          >
            <Compass className="text-primary" size={22} strokeWidth={2.4} />
            Waypoint
          </Link>
          <h1 className="font-display text-2xl font-bold text-ink">{title}</h1>
          <p className="text-sm text-ink-soft mt-1.5">{subtitle}</p>
          <div className="mt-8">{children}</div>
        </div>
      </div>
    </div>
  );
}
