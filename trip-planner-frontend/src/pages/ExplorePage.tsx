import { useEffect, useState } from "react";
import { Search } from "lucide-react";
import DestinationCard from "../components/ui/DestinationCard";
import { LoadingState, ErrorState, EmptyState } from "../components/ui/States";
import Button from "../components/ui/Button";
import { searchDestinations, getPopularDestinations } from "../services/exploreService";
import { getErrorMessage } from "../services/api";
import type { DestinationResponse } from "../types/explore";

const CATEGORIES = ["BEACH", "MOUNTAIN", "HISTORY", "NATURE", "CITY"];

// Deterministic image per destination, matching the DestinationCard/
// LandingPage placeholder-photo approach (see those files for why).
function seedFor(id: number) {
  return `waypoint-explore-${id}`;
}

export default function ExplorePage() {
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [destinations, setDestinations] = useState<DestinationResponse[]>([]);
  const [hasMore, setHasMore] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Debounce search input so we're not firing a request per keystroke.
    const timer = setTimeout(() => {
      setPage(0);
      fetchDestinations(0, true);
    }, 350);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, category]);

  async function fetchDestinations(pageToLoad: number, replace: boolean) {
    setIsLoading(true);
    setError(null);
    try {
      // With no search/category filter, hit the dedicated /popular endpoint
      // explicitly rather than relying on search()'s default ordering
      // happening to match - keeps this correct even if that backend
      // implementation detail ever changes.
      const result = !query && !category
        ? await getPopularDestinations(pageToLoad, 12)
        : await searchDestinations(query || undefined, category ?? undefined, pageToLoad, 12);
      setDestinations((prev) => (replace ? result.content : [...prev, ...result.content]));
      setHasMore(!result.last);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }

  function loadMore() {
    const nextPage = page + 1;
    setPage(nextPage);
    fetchDestinations(nextPage, false);
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-10">
      <h1 className="font-display text-3xl font-bold text-ink">Explore destinations</h1>
      <p className="text-ink-soft mt-1.5">Browse places to inspire your next trip.</p>

      <div className="flex items-center gap-2 mt-6 bg-bg-alt rounded-full px-4 py-2.5 max-w-md">
        <Search size={16} className="text-ink-soft shrink-0" />
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search destinations..."
          className="bg-transparent outline-none text-sm w-full text-ink placeholder:text-ink-soft"
        />
      </div>

      <div className="flex flex-wrap gap-2 mt-4">
        <button
          onClick={() => setCategory(null)}
          className={`px-3.5 py-1.5 rounded-full text-xs font-semibold transition-colors ${
            category === null ? "bg-ink text-white" : "bg-bg-alt text-ink hover:bg-border"
          }`}
        >
          All
        </button>
        {CATEGORIES.map((c) => (
          <button
            key={c}
            onClick={() => setCategory(c)}
            className={`px-3.5 py-1.5 rounded-full text-xs font-semibold capitalize transition-colors ${
              category === c ? "bg-ink text-white" : "bg-bg-alt text-ink hover:bg-border"
            }`}
          >
            {c.toLowerCase()}
          </button>
        ))}
      </div>

      <div className="mt-8">
        {isLoading && destinations.length === 0 ? (
          <LoadingState message="Finding destinations..." />
        ) : error ? (
          <ErrorState message={error} onRetry={() => fetchDestinations(0, true)} />
        ) : destinations.length === 0 ? (
          <EmptyState title="No destinations found" description="Try a different search or category." />
        ) : (
          <>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-5 sm:gap-8">
              {destinations.map((d) => (
                <DestinationCard
                  key={d.id}
                  name={d.name}
                  country={d.country}
                  category={d.category ?? "Destination"}
                  imageSeed={seedFor(d.id)}
                  to={`/explore/${d.id}`}
                />
              ))}
            </div>
            {hasMore && (
              <div className="flex justify-center mt-10">
                <Button variant="secondary" onClick={loadMore} isLoading={isLoading}>
                  Load more
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
