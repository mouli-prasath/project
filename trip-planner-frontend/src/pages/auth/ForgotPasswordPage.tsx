import { useState } from "react";
import type { FormEvent } from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, Construction } from "lucide-react";
import AuthShell from "../../layouts/AuthShell";
import Input from "../../components/ui/Input";
import Button from "../../components/ui/Button";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [submitted, setSubmitted] = useState(false);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitted(true);
  }

  return (
    <AuthShell title="Reset your password" subtitle="Enter your registered email address.">
      {submitted ? (
        <div className="flex flex-col items-center text-center gap-3 py-6">
          <Construction className="text-warning" size={28} />
          <p className="text-sm text-ink-soft leading-relaxed">
            Password reset isn't connected to a real email service yet. This
            screen is a placeholder for that flow &mdash; nothing was sent to{" "}
            <span className="font-medium text-ink">{email}</span>.
          </p>
          <Link to="/login" className="text-sm text-primary hover:underline mt-2">
            Back to login
          </Link>
        </div>
      ) : (
        <>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <Input label="Email" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
            <Button type="submit" className="mt-1">Send reset link</Button>
          </form>
          <Link to="/login" className="flex items-center justify-center gap-1.5 text-sm text-ink-soft hover:text-primary mt-6">
            <ArrowLeft size={14} />
            Back to login
          </Link>
        </>
      )}
    </AuthShell>
  );
}
