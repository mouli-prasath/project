import type { ReactNode } from "react";
import { Loader2, AlertCircle, Compass } from "lucide-react";
import Button from "./Button";

export function LoadingState({ message = "Loading..." }: { message?: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16 text-ink-soft">
      <Loader2 className="animate-spin text-primary" size={28} />
      <p className="text-sm">{message}</p>
    </div>
  );
}

export function ErrorState({
  message = "Something went wrong.",
  onRetry,
}: {
  message?: string;
  onRetry?: () => void;
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16 text-center">
      <AlertCircle className="text-error" size={28} />
      <p className="text-sm text-ink-soft max-w-sm">{message}</p>
      {onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  );
}

export function EmptyState({
  title,
  description,
  action,
  icon,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
  icon?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16 text-center px-4">
      <div className="text-primary/40">{icon ?? <Compass size={32} />}</div>
      <h3 className="font-display text-lg font-semibold text-ink">{title}</h3>
      {description && <p className="text-sm text-ink-soft max-w-sm">{description}</p>}
      {action}
    </div>
  );
}
