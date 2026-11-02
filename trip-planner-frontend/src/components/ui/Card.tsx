import type { HTMLAttributes, ReactNode } from "react";

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  padding?: "none" | "sm" | "md" | "lg";
}

const paddingClasses = {
  none: "",
  sm: "p-4",
  md: "p-6",
  lg: "p-8",
};

export default function Card({ children, padding = "md", className = "", ...rest }: CardProps) {
  return (
    <div
      className={`bg-surface rounded-[var(--radius-card)] shadow-[0_2px_16px_rgba(34,41,47,0.08)] ${paddingClasses[padding]} ${className}`}
      {...rest}
    >
      {children}
    </div>
  );
}
