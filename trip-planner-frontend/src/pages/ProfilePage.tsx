import { useNavigate } from "react-router-dom";
import { LogOut } from "lucide-react";
import Button from "../components/ui/Button";
import Card from "../components/ui/Card";
import { useAuth } from "../context/AuthContext";

function initials(name: string): string {
  return name
    .split(" ")
    .map((p) => p[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

export default function ProfilePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) return null;

  function handleLogout() {
    logout();
    navigate("/");
  }

  return (
    <div className="max-w-lg mx-auto px-4 sm:px-6 py-10">
      <h1 className="font-display text-3xl font-bold text-ink">Profile</h1>

      <Card padding="lg" className="mt-6">
        <div className="flex items-center gap-4">
          <div className="w-16 h-16 rounded-full bg-primary text-white flex items-center justify-center font-display font-bold text-xl shrink-0">
            {initials(user.name)}
          </div>
          <div>
            <p className="font-display font-semibold text-lg text-ink">{user.name}</p>
            <p className="text-sm text-ink-soft">{user.email}</p>
          </div>
        </div>

        <div className="border-t border-border mt-6 pt-6 flex flex-col gap-3 text-sm">
          <div className="flex justify-between">
            <span className="text-ink-soft">Member since</span>
            <span className="text-ink font-medium">
              {new Date(user.createdAt).toLocaleDateString("en-US", { month: "long", year: "numeric" })}
            </span>
          </div>
        </div>

        {/* Deliberately no "edit profile" / "change password" / "travel
            preferences" sections here - the backend has no endpoints for
            any of these yet, and building UI that looks functional but
            silently does nothing would be exactly the kind of fake
            functionality the project spec warns against. */}

        <Button variant="secondary" icon={<LogOut size={15} />} onClick={handleLogout} className="w-full mt-6">
          Log out
        </Button>
      </Card>
    </div>
  );
}
