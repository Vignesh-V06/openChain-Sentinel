import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
} from "react-router-dom";

import Sidebar from "./components/layout/Sidebar";
import Topbar from "./components/layout/Topbar";

import DashboardEntry from "./pages/DashboardEntry";
import Dashboard from "./pages/Dashboard";
import Projects from "./pages/Projects";
import ProjectDetails from "./pages/ProjectDetails";
import ScanView from "./pages/ScanView";
import ScanHistory from "./pages/ScanHistory";


function AppLayout({ children }) {

  return (
    <div className="app">

      <Sidebar />

      <div className="app-content">

        <Topbar />

        {children}

      </div>

    </div>
  );
}


function PlaceholderPage({
  title,
  description,
}) {

  return (
    <main className="dashboard-page">

      <section className="page-heading">

        <div>

          <div className="eyebrow">
            OPENCHAIN SENTINEL
          </div>

          <h1>
            {title}
          </h1>

          <p>
            {description}
          </p>

        </div>

      </section>


      <div className="panel">

        <div className="placeholder-content">

          <h3>
            {title}
          </h3>

          <p>
            This section will be connected
            to the security analysis workflow.
          </p>

        </div>

      </div>

    </main>
  );
}


function App() {

  return (
    <BrowserRouter>

      <Routes>

        {/* ROOT */}

        <Route
          path="/"
          element={
            <Navigate
              to="/projects"
              replace
            />
          }
        />


        {/* DASHBOARD ENTRY */}

        <Route
          path="/dashboard"
          element={
            <AppLayout>
              <DashboardEntry />
            </AppLayout>
          }
        />


        {/* ACTUAL DASHBOARD */}

        <Route
          path="/projects/:projectId/scans/:scanId"
          element={
            <AppLayout>
              <ScanView />
            </AppLayout>
          }
        />


        {/* PROJECTS */}

        <Route
          path="/projects"
          element={
            <AppLayout>
              <Projects />
            </AppLayout>
          }
        />


        {/* PROJECT DETAILS */}

        <Route
          path="/projects/:projectId"
          element={
            <AppLayout>
              <ProjectDetails />
            </AppLayout>
          }
        />


        {/* PROJECT SCAN HISTORY */}

        <Route
          path="/projects/:projectId/scans"
          element={
            <AppLayout>
              <ScanHistory />
            </AppLayout>
          }
        />


        {/* SECURITY */}

        <Route
          path="/vulnerabilities"
          element={
            <AppLayout>
              <PlaceholderPage
                title="Vulnerabilities"
                description="Investigate vulnerabilities detected across your projects."
              />
            </AppLayout>
          }
        />


        <Route
          path="/dependency-graph"
          element={
            <AppLayout>
              <PlaceholderPage
                title="Dependency Graph"
                description="Explore dependency relationships and affected paths."
              />
            </AppLayout>
          }
        />


        <Route
          path="/risk-analysis"
          element={
            <AppLayout>
              <PlaceholderPage
                title="Risk Analysis"
                description="Review contextual project-level risk assessments."
              />
            </AppLayout>
          }
        />


        {/* GLOBAL SCAN HISTORY */}

        <Route
          path="/scan-history"
          element={
            <AppLayout>
              <ScanHistory />
            </AppLayout>
          }
        />


        {/* SETTINGS */}

        <Route
          path="/settings"
          element={
            <AppLayout>
              <PlaceholderPage
                title="Settings"
                description="Configure OpenChain Sentinel preferences."
              />
            </AppLayout>
          }
        />


        {/* FALLBACK */}

        <Route
          path="*"
          element={
            <Navigate
              to="/projects"
              replace
            />
          }
        />

      </Routes>

    </BrowserRouter>
  );
}


export default App;