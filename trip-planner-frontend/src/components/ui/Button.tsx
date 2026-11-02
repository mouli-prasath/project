import type { ButtonHTMLAttributes, ReactNode } from "react";
import { Loader2 } from "lucide-react";

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger" | "white";
type ButtonSize = "sm" | "md" | "lg";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  isLoading?: boolean;
  icon?: ReactNode;
}

const variantClasses: Record<ButtonVariant, string> = {
  primary:
    "bg-primary text-white hover:bg-primary-hover disabled:opacity-50 shadow-sm",
  secondary:
    "bg-white text-ink border-2 border-ink hover:bg-bg-alt disabled:opacity-50",
  ghost:
    "bg-transparent text-ink hover:bg-primary-soft disabled:opacity-50",
  danger:
    "bg-white text-error border-2 border-error/40 hover:bg-error-soft disabled:opacity-50",
  white:
    "bg-white text-primary hover:bg-white/90 disabled:opacity-50 shadow-sm",
};

const sizeClasses: Record<ButtonSize, string> = {
  sm: "text-sm px-4 py-2 gap-1.5",
  md: "text-sm px-5 py-2.5 gap-2",
  lg: "text-base px-7 py-3.5 gap-2.5",
};

export default function Button({
  variant = "primary",
  size = "md",
  isLoading = false,
  icon,
  disabled,
  children,
  className = "",
  ...rest
}: ButtonProps) {
  return (
    <button
      disabled={disabled || isLoading}
      className={`inline-flex items-center justify-center rounded-full font-semibold
        transition-all duration-150 cursor-pointer disabled:cursor-not-allowed
        hover:scale-[1.02] active:scale-[0.98]
        ${variantClasses[variant]} ${sizeClasses[size]} ${className}`}
      {...rest}
    >
      {isLoading ? (
        <Loader2 className="animate-spin" size={16} />
      ) : (
        icon
      )}
      {children}
    </button>
  );
}
