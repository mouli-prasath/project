import { Link } from "react-router-dom";

interface DestinationCardProps {
  name: string;
  country: string;
  category: string;
  imageSeed: string;
  to?: string; // defaults to /register (landing page, anonymous visitor);
               // Explore page overrides this to /trips/new?destination=... for logged-in users
}

// NOTE: images are from Lorem Picsum (picsum.photos) - real photography,
// reliably-working deterministic URLs, but NOT guaranteed to literally
// depict the named destination. Placeholder choice - see README. Swap for
// licensed destination photography or a real Unsplash API integration
// before any real deployment.
//
// Deliberately no star rating: the backend's Destination entity only has a
// popularityScore, not real user reviews - a fabricated rating would imply
// data that doesn't exist. A category tag gives the same visual accent
// without faking a trust signal.
function imageUrl(seed: string, width: number, height: number) {
  return `https://picsum.photos/seed/${seed}/${width}/${height}`;
}

export default function DestinationCard({ name, country, category, imageSeed, to }: DestinationCardProps) {
  return (
    <Link to={to ?? "/register"} className="group block">
      <div className="relative aspect-[4/3] rounded-[var(--radius-card)] overflow-hidden">
        <img
          src={imageUrl(imageSeed, 400, 300)}
          alt={`${name}, ${country}`}
          loading="lazy"
          className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
        />
        <span className="absolute top-3 left-3 bg-white/95 rounded-full px-2.5 py-1 text-xs font-semibold text-ink">
          {category}
        </span>
      </div>
      <p className="font-display font-semibold text-ink mt-3">{name}</p>
      <p className="text-sm text-ink-soft">{country}</p>
    </Link>
  );
}
