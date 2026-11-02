import { useEffect, useState } from "react";
import { Heart, MapPin, Trash2 } from "lucide-react";
import Button from "../components/ui/Button";
import { LoadingState, ErrorState, EmptyState } from "../components/ui/States";
import { listFavorites, removeFavorite } from "../services/favoriteService";
import { getErrorMessage } from "../services/api";
import type { FavoriteResponse } from "../types/favorite";

export default function FavoritesPage() {
  const [favorites, setFavorites] = useState<FavoriteResponse[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [removingId, setRemovingId] = useState<number | null>(null);

  useEffect(() => {
    fetchFavorites(0, true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function fetchFavorites(pageToLoad: number, replace: boolean) {
    setIsLoading(true);
    setError(null);
    try {
      const result = await listFavorites(pageToLoad, 12);
      setFavorites((prev) => (replace ? result.content : [...prev, ...result.content]));
      setHasMore(!result.last);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleRemove(id: number) {
    setRemovingId(id);
    try {
      await removeFavorite(id);
      setFavorites((prev) => prev.filter((f) => f.id !== id));
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setRemovingId(null);
    }
  }

  function loadMore() {
    const next = page + 1;
    setPage(next);
    fetchFavorites(next, false);
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-10">
      <h1 className="font-display text-3xl font-bold text-ink">Favorites</h1>
      <p className="text-ink-soft mt-1.5">Places you've saved while exploring.</p>

      <div className="mt-8">
        {isLoading && favorites.length === 0 ? (
          <LoadingState message="Loading your favorites..." />
        ) : error ? (
          <ErrorState message={error} onRetry={() => fetchFavorites(0, true)} />
        ) : favorites.length === 0 ? (
          <EmptyState
            icon={<Heart size={32} />}
            title="No favorites yet"
            description="Save places you like while browsing a trip's nearby places or hotels."
          />
        ) : (
          <>
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
              {favorites.map((f) => (
                <div key={f.id} className="border border-border rounded-[var(--radius-card)] p-4">
                  <div className="flex items-start justify-between gap-2">
                    <h4 className="font-display font-semibold text-ink">{f.placeName}</h4>
                    <button
                      onClick={() => handleRemove(f.id)}
                      disabled={removingId === f.id}
                      aria-label="Remove favorite"
                      className="text-ink-soft hover:text-error shrink-0 disabled:opacity-40"
                    >
                      <Trash2 size={15} />
                    </button>
                  </div>
                  {f.category && (
                    <span className="text-[11px] bg-teal-soft text-teal rounded-full px-2 py-0.5 capitalize inline-block mt-2">
                      {f.category}
                    </span>
                  )}
                  {f.address && (
                    <p className="flex items-center gap-1 text-xs text-ink-soft mt-2">
                      <MapPin size={11} />
                      {f.address}
                    </p>
                  )}
                </div>
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
