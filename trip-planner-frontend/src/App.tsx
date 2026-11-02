import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import PublicLayout from "./layouts/PublicLayout";
import ProtectedRoute from "./components/layout/ProtectedRoute";

import LandingPage from "./pages/LandingPage";
import LoginPage from "./pages/auth/LoginPage";
import RegisterPage from "./pages/auth/RegisterPage";
import ForgotPasswordPage from "./pages/auth/ForgotPasswordPage";
import DashboardPage from "./pages/DashboardPage";
import ExplorePage from "./pages/ExplorePage";
import DestinationDetailPage from "./pages/DestinationDetailPage";
import MyTripsPage from "./pages/MyTripsPage";
import CreateTripWizard from "./pages/CreateTripWizard";
import GeneratingPage from "./pages/GeneratingPage";
import TripDetailPage from "./pages/TripDetailPage";
import ProfilePage from "./pages/ProfilePage";
import FavoritesPage from "./pages/FavoritesPage";
import SharedTripPage from "./pages/SharedTripPage";

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Auth pages use their own full-screen AuthShell, no navbar/footer */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />

          {/* Public share link - no navbar/footer, no auth required */}
          <Route path="/shared/trips/:shareToken" element={<SharedTripPage />} />

          {/* Everything else uses the standard nav+footer layout */}
          <Route path="/" element={<PublicLayout><LandingPage /></PublicLayout>} />
          <Route path="/explore" element={<PublicLayout><ExplorePage /></PublicLayout>} />
          <Route path="/explore/:id" element={<PublicLayout><DestinationDetailPage /></PublicLayout>} />

          <Route
            path="/dashboard"
            element={
              <PublicLayout>
                <ProtectedRoute><DashboardPage /></ProtectedRoute>
              </PublicLayout>
            }
          />
          <Route
            path="/trips"
            element={
              <PublicLayout>
                <ProtectedRoute><MyTripsPage /></ProtectedRoute>
              </PublicLayout>
            }
          />
          <Route
            path="/trips/new"
            element={
              <PublicLayout>
                <ProtectedRoute><CreateTripWizard /></ProtectedRoute>
              </PublicLayout>
            }
          />
          <Route
            path="/trips/:tripId/generating"
            element={
              <PublicLayout>
                <ProtectedRoute><GeneratingPage /></ProtectedRoute>
              </PublicLayout>
            }
          />
          <Route
            path="/trips/:tripId"
            element={
              <PublicLayout>
                <ProtectedRoute><TripDetailPage /></ProtectedRoute>
              </PublicLayout>
            }
          />
          <Route
            path="/favorites"
            element={
              <PublicLayout>
                <ProtectedRoute><FavoritesPage /></ProtectedRoute>
              </PublicLayout>
            }
          />
          <Route
            path="/profile"
            element={
              <PublicLayout>
                <ProtectedRoute><ProfilePage /></ProtectedRoute>
              </PublicLayout>
            }
          />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
