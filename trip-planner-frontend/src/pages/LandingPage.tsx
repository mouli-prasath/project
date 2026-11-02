import { Link } from "react-router-dom";
import { MapPin, Sparkles, Route } from "lucide-react";
import Button from "../components/ui/Button";
import DestinationCard from "../components/ui/DestinationCard";

const DESTINATIONS = [
  { name: "Goa", country: "India", category: "Beaches", imageSeed: "goa-waypoint" },
  { name: "Kerala", country: "India", category: "Nature", imageSeed: "kerala-waypoint" },
  { name: "Bali", country: "Indonesia", category: "Beaches", imageSeed: "bali-waypoint" },
  { name: "Paris", country: "France", category: "City", imageSeed: "paris-waypoint" },
  { name: "Kyoto", country: "Japan", category: "History", imageSeed: "kyoto-waypoint" },
  { name: "Manali", country: "India", category: "Mountains", imageSeed: "manali-waypoint" },
];

const STEPS = [
  { icon: MapPin, color: "text-primary", bg: "bg-primary-soft", title: "Tell us where and when", description: "Destination, dates, travelers, budget, and what you're into." },
  { icon: Sparkles, color: "text-teal", bg: "bg-teal-soft", title: "AI drafts your itinerary", description: "A day-by-day plan built from real, nearby places matched to your interests." },
  { icon: Route, color: "text-primary", bg: "bg-primary-soft", title: "Fine-tune and go", description: "Reorder stops, swap activities, or regenerate a day you don't like." },
];

function heroImageUrl(seed: string, width: number, height: number) {
  return `https://picsum.photos/seed/${seed}/${width}/${height}`;
}

export default function LandingPage() {
  return (
    <div>
      <section className="max-w-6xl mx-auto px-4 sm:px-6 pt-8">
        <div className="relative rounded-[28px] overflow-hidden aspect-[16/10] sm:aspect-[16/8]">
          <img src={heroImageUrl("waypoint-hero-travel", 1600, 900)} alt="Scenic travel destination" className="absolute inset-0 w-full h-full object-cover" />
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-black/10 to-transparent" />
          <div className="absolute inset-0 flex flex-col justify-end p-6 sm:p-12">
            <h1 className="font-display text-3xl sm:text-5xl font-bold text-white leading-tight max-w-xl">
              Plan your perfect trip with AI.
            </h1>
            <p className="text-white/90 mt-3 max-w-md text-sm sm:text-base">
              Real places, real routes, personalized to you.
            </p>
            <Link to="/register" className="inline-block mt-6 w-fit">
              <Button size="lg">Plan my trip</Button>
            </Link>
          </div>
        </div>
      </section>

      <section className="max-w-6xl mx-auto px-4 sm:px-6 py-16">
        <h2 className="font-display text-2xl sm:text-3xl font-bold text-ink mb-8">Popular destinations</h2>
        <div className="grid grid-cols-2 md:grid-cols-3 gap-5 sm:gap-8">
          {DESTINATIONS.map((d) => <DestinationCard key={d.name} {...d} />)}
        </div>
      </section>

      <section className="bg-bg-alt py-16">
        <div className="max-w-6xl mx-auto px-4 sm:px-6">
          <h2 className="font-display text-2xl sm:text-3xl font-bold text-ink mb-10 text-center">How it works</h2>
          <div className="grid sm:grid-cols-3 gap-8">
            {STEPS.map((s) => (
              <div key={s.title} className="text-center">
                <div className={`w-14 h-14 rounded-full ${s.bg} flex items-center justify-center mx-auto`}>
                  <s.icon className={s.color} size={24} />
                </div>
                <h3 className="font-display font-semibold text-ink mt-4">{s.title}</h3>
                <p className="text-sm text-ink-soft mt-2 leading-relaxed max-w-xs mx-auto">{s.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="max-w-6xl mx-auto px-4 sm:px-6 py-16">
        <div className="relative rounded-[28px] overflow-hidden py-16 px-6 sm:px-16 text-center">
          <img src={heroImageUrl("waypoint-cta-travel", 1600, 700)} alt="" className="absolute inset-0 w-full h-full object-cover" />
          <div className="absolute inset-0 bg-primary/80" />
          <div className="relative">
            <h2 className="font-display text-2xl sm:text-3xl font-bold text-white">Your next trip is a few minutes away.</h2>
            <Link to="/register" className="inline-block mt-6">
              <Button size="lg" variant="white">Plan my trip</Button>
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
