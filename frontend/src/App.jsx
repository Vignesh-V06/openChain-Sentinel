
import {
  BrowserRouter,
  Navigate,
  Outlet,
  Route,
  Routes,
} from "react-router-dom";

import AppShell from "./components/layout/AppShell";

import DashboardEntry from "./pages/DashboardEntry";
import Projects from "./pages/Projects";
import ProjectDetails from "./pages/ProjectDetails";
import ScanHistory from "./pages/ScanHistory";
import ScanView from "./pages/ScanView";
import VulnerabilityIntelligence from "./pages/VulnerabilityIntelligence";
import DependencyGraph from "./pages/DependencyGraph";
import RiskAnalysis from "./pages/RiskAnalysis";
import Settings from "./pages/Settings";
import AuthPage from "./pages/AuthPage";

import { AuthProvider, useAuth } from "./context/AuthContext";

function AuthLoadingScreen() {
  return (
    <div className="auth-loading">
      <span className="auth-spinner">◌</span>
      <span>Checking your session...</span>
    </div>
  );
}

function RootRedirect() {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return <AuthLoadingScreen />;
  }

  return (
    <Navigate
      to={isAuthenticated ? "/dashboard" : "/login"}
      replace
    />
  );
}

function GuestOnlyRoute({ mode }) {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return <AuthLoadingScreen />;
  }

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return <AuthPage mode={mode} />;
}

function AuthenticatedLayout() {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return <AuthLoadingScreen />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <AppShell>
      <Outlet />
    </AppShell>
  );
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<RootRedirect />} />

      <Route
        path="/login"
        element={<GuestOnlyRoute mode="login" />}
      />

      <Route
        path="/register"
        element={<GuestOnlyRoute mode="register" />}
      />

      <Route element={<AuthenticatedLayout />}>
        <Route path="/dashboard" element={<DashboardEntry />} />

        <Route path="/projects" element={<Projects />} />

        <Route
          path="/projects/:projectId"
          element={<ProjectDetails />}
        />

        <Route
          path="/projects/:projectId/scans"
          element={<ScanHistory />}
        />

        <Route
          path="/projects/:projectId/scans/:scanId"
          element={<ScanView />}
        />

        <Route
          path="/vulnerabilities"
          element={<VulnerabilityIntelligence />}
        />

        <Route
          path="/dependency-graph"
          element={<DependencyGraph />}
        />

        <Route
          path="/risk-analysis"
          element={<RiskAnalysis />}
        />

        <Route
          path="/scan-history"
          element={<ScanHistory />}
        />

        <Route path="/settings" element={<Settings />} />
      </Route>

      <Route path="*" element={<RootRedirect />} />
    </Routes>
  );
}

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
