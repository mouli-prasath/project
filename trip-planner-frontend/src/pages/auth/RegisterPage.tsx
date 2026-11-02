import { useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import AuthShell from "../../layouts/AuthShell";
import Input from "../../components/ui/Input";
import Button from "../../components/ui/Button";
import { register } from "../../services/authService";
import { getErrorMessage } from "../../services/api";
import { useAuth } from "../../context/AuthContext";

interface FieldErrors {
  name?: string;
  email?: string;
  password?: string;
  confirmPassword?: string;
  terms?: string;
}

export default function RegisterPage() {
  const navigate = useNavigate();
  const { setUser } = useAuth();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [agreedToTerms, setAgreedToTerms] = useState(false);

  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  function validate(): boolean {
    const errors: FieldErrors = {};
    if (!name.trim()) errors.name = "Name is required";
    if (!/^\S+@\S+\.\S+$/.test(email)) errors.email = "Enter a valid email";
    if (password.length < 8) errors.password = "Password must be at least 8 characters";
    if (confirmPassword !== password) errors.confirmPassword = "Passwords don't match";
    if (!agreedToTerms) errors.terms = "You must agree to the terms to continue";
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    if (!validate()) return;
    setIsSubmitting(true);
    try {
      const response = await register({ name, email, password });
      setUser(response.user);
      navigate("/dashboard");
    } catch (error) {
      setFormError(getErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthShell title="Create your account" subtitle="Start planning your trip in a couple of minutes.">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
        <Input label="Full name" autoComplete="name" value={name} onChange={(e) => setName(e.target.value)} error={fieldErrors.name} />
        <Input label="Email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} error={fieldErrors.email} />
        <Input
          label="Password"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={fieldErrors.password}
          hint={!fieldErrors.password ? "At least 8 characters" : undefined}
        />
        <Input label="Confirm password" type="password" autoComplete="new-password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} error={fieldErrors.confirmPassword} />

        <label className="flex items-start gap-2.5 text-sm text-ink-soft">
          <input type="checkbox" checked={agreedToTerms} onChange={(e) => setAgreedToTerms(e.target.checked)} className="mt-0.5 accent-primary" />
          <span>
            I agree to the <Link to="/terms" className="text-primary hover:underline">Terms</Link> and{" "}
            <Link to="/privacy" className="text-primary hover:underline">Privacy Policy</Link>
          </span>
        </label>
        {fieldErrors.terms && <p className="text-xs text-error -mt-2">{fieldErrors.terms}</p>}

        {formError && <p role="alert" className="text-sm text-error bg-error-soft rounded-2xl px-3 py-2">{formError}</p>}

        <Button type="submit" isLoading={isSubmitting} className="mt-1">
          Create account
        </Button>
      </form>

      <p className="text-sm text-ink-soft text-center mt-6">
        Already have an account?{" "}
        <Link to="/login" className="text-primary font-medium hover:underline">Log in</Link>
      </p>
    </AuthShell>
  );
}
