import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Menu, X, Compass } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import Button from "../ui/Button";

export default function Navbar() {
  const { isAuthenticated, user, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    setMobileOpen(false);
    navigate("/");
  }

  const loggedOutLinks = (
    <>
      <Link to="/" className="nav-link">Home</Link>
      <Link to="/explore" className="nav-link">Explore</Link>
    </>
  );

  const loggedInLinks = (
    <>
      <Link to="/dashboard" className="nav-link">Dashboard</Link>
      <Link to="/trips/new" className="nav-link">Plan Trip</Link>
      <Link to="/trips" className="nav-link">My Trips</Link>
      <Link to="/favorites" className="nav-link">Favorites</Link>
      <Link to="/explore" className="nav-link">Explore</Link>
    </>
  );

  return (
    <header className="sticky top-0 z-40 bg-bg/90 backdrop-blur border-b border-border">
      <nav className="max-w-6xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2 font-display text-lg font-bold text-ink">
          <Compass className="text-primary" size={24} strokeWidth={2.4} />
          Waypoint
        </Link>

        <div className="hidden md:flex items-center gap-6">
          {isAuthenticated ? loggedInLinks : loggedOutLinks}
        </div>

        <div className="hidden md:flex items-center gap-3">
          {isAuthenticated ? (
            <>
              <Link
                to="/profile"
                className="text-sm font-medium text-ink hover:text-primary transition-colors"
              >
                {user?.name?.split(" ")[0] ?? "Profile"}
              </Link>
              <Button variant="secondary" size="sm" onClick={handleLogout}>
                Log out
              </Button>
            </>
          ) : (
            <>
              <Link to="/login" className="text-sm font-medium text-ink hover:text-primary transition-colors">
                Log in
              </Link>
              <Link to="/register">
                <Button size="sm">Get Started</Button>
              </Link>
            </>
          )}
        </div>

        <button
          className="md:hidden text-ink"
          onClick={() => setMobileOpen((o) => !o)}
          aria-label={mobileOpen ? "Close menu" : "Open menu"}
        >
          {mobileOpen ? <X size={22} /> : <Menu size={22} />}
        </button>
      </nav>

      {mobileOpen && (
        <div className="md:hidden border-t border-border bg-bg px-4 py-4 flex flex-col gap-4">
          <div className="flex flex-col gap-3">
            {isAuthenticated ? loggedInLinks : loggedOutLinks}
          </div>
          <div className="flex flex-col gap-2 pt-2 border-t border-border">
            {isAuthenticated ? (
              <>
                <Link to="/profile" onClick={() => setMobileOpen(false)} className="nav-link">
                  Profile
                </Link>
                <Button variant="secondary" size="sm" onClick={handleLogout}>
                  Log out
                </Button>
              </>
            ) : (
              <>
                <Link to="/login" onClick={() => setMobileOpen(false)} className="nav-link">
                  Log in
                </Link>
                <Link to="/register" onClick={() => setMobileOpen(false)}>
                  <Button size="sm" className="w-full">Get Started</Button>
                </Link>
              </>
            )}
          </div>
        </div>
      )}
    </header>
  );
}
