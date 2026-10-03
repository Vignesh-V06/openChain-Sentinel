import {
  useEffect,
  useState,
} from "react";

import {
  useParams,
  Navigate,
} from "react-router-dom";

import {
  getVulnerabilities,
  getRiskAssessments,
  getScanSummary,
  getScanDependencies,
  getHealth,
} from "../services/api";

import {
  Package,
  ShieldAlert,
  Boxes,
  GitBranch,
} from "lucide-react";

import StatCard from "../components/dashboard/StatCard";
import RiskScoreCard from "../components/dashboard/RiskScoreCard";
import SeverityChart from "../components/dashboard/SeverityChart";
import VulnerabilityTable from "../components/dashboard/VulnerabilityTable";
import VulnerabilityDrawer from "../components/vulnerability/VulnerabilityDrawer";

function Dashboard() {
  const {
    projectId,
    scanId,
  } = useParams();

  const [summary, setSummary] =
    useState(null);

  const [vulnerabilities, setVulnerabilities] =
    useState([]);

  const [riskAssessments, setRiskAssessments] =
    useState([]);

  const [dependencies, setDependencies] =
    useState([]);

  const [selectedVulnerability, setSelectedVulnerability] =
    useState(null);

  const [selectedRisk, setSelectedRisk] =
    useState(null);

  const [backendHealthy, setBackendHealthy] =
    useState(false);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState(null);


  /*
   * =====================================================
   * LOAD DASHBOARD
   * =====================================================
   */

  useEffect(() => {

    if (!projectId || !scanId) {
      return;
    }

    const loadDashboard = async () => {

      try {

        setLoading(true);
        setError(null);


        /*
         * BACKEND HEALTH
         */

        try {

          await getHealth();

          setBackendHealthy(true);

        } catch {

          setBackendHealthy(false);

        }


        /*
         * FETCH DATA
         */

        const [
          vulnerabilityResponse,
          riskResponse,
          summaryResponse,
          dependencyResponse,
        ] = await Promise.all([

          getVulnerabilities(
            projectId,
            scanId
          ),

          getRiskAssessments(
            projectId,
            scanId
          ),

          getScanSummary(
            projectId,
            scanId
          ),

          getScanDependencies(
            projectId,
            scanId
          ),

        ]);


        /*
         * VULNERABILITIES
         */

        let vulnerabilityData = [];

        if (
          Array.isArray(
            vulnerabilityResponse
          )
        ) {

          vulnerabilityData =
            vulnerabilityResponse;

        } else if (
          Array.isArray(
            vulnerabilityResponse?.storedVulnerabilities
          )
        ) {

          vulnerabilityData =
            vulnerabilityResponse
              .storedVulnerabilities;

        } else if (
          Array.isArray(
            vulnerabilityResponse?.vulnerabilities
          )
        ) {

          vulnerabilityData =
            vulnerabilityResponse
              .vulnerabilities;

        } else if (
          Array.isArray(
            vulnerabilityResponse?.data
          )
        ) {

          vulnerabilityData =
            vulnerabilityResponse.data;

        }


        /*
         * RISK
         */

        let riskData = [];

        if (
          Array.isArray(riskResponse)
        ) {

          riskData =
            riskResponse;

        } else if (
          Array.isArray(
            riskResponse?.riskAssessments
          )
        ) {

          riskData =
            riskResponse.riskAssessments;

        } else if (
          Array.isArray(
            riskResponse?.assessments
          )
        ) {

          riskData =
            riskResponse.assessments;

        } else if (
          Array.isArray(
            riskResponse?.data
          )
        ) {

          riskData =
            riskResponse.data;

        }


        /*
         * DEPENDENCIES
         */

        const dependencyData =
          Array.isArray(
            dependencyResponse
          )
            ? dependencyResponse
            : [];


        /*
         * SAVE
         */

        setVulnerabilities(
          vulnerabilityData
        );

        setRiskAssessments(
          riskData
        );

        setDependencies(
          dependencyData
        );

        setSummary(
          summaryResponse
        );


        console.log(
          "Dashboard project:",
          projectId
        );

        console.log(
          "Dashboard scan:",
          scanId
        );

        console.log(
          "Vulnerabilities:",
          vulnerabilityData
        );

        console.log(
          "Risk assessments:",
          riskData
        );

        console.log(
          "Dependencies:",
          dependencyData
        );

        console.log(
          "Summary:",
          summaryResponse
        );

      } catch (err) {

        console.error(
          "Failed to load dashboard:",
          err
        );

        setError(
          err?.response?.data?.message ||
          err?.message ||
          "Failed to load dashboard data."
        );

      } finally {

        setLoading(false);

      }

    };

    loadDashboard();

  }, [projectId, scanId]);


  /*
   * =====================================================
   * INVALID ROUTE
   * =====================================================
   */

  if (
    !projectId ||
    !scanId
  ) {

    return (
      <Navigate
        to="/projects"
        replace
      />
    );

  }


  /*
   * =====================================================
   * DRAWER
   * =====================================================
   */

  const handleSelectVulnerability = (
    vulnerability,
    risk
  ) => {

    setSelectedVulnerability(
      vulnerability
    );

    setSelectedRisk(risk);

  };


  const handleCloseDrawer = () => {

    setSelectedVulnerability(null);

    setSelectedRisk(null);

  };


  /*
   * =====================================================
   * LOADING
   * =====================================================
   */

  if (loading) {

    return (
      <main className="dashboard-page">

        <div className="page-loading">

          <div className="loading-spinner" />

          <p>
            Loading security posture...
          </p>

        </div>

      </main>
    );

  }


  /*
   * =====================================================
   * ERROR
   * =====================================================
   */

  if (error) {

    return (
      <main className="dashboard-page">

        <div className="page-error">

          <ShieldAlert size={30} />

          <h2>
            Unable to load dashboard
          </h2>

          <p>
            {error}
          </p>

        </div>

      </main>
    );

  }


  /*
   * =====================================================
   * SUMMARY
   * =====================================================
   */

  const riskScore =
    summary?.highestRiskScore ?? 0;

  const overallRisk =
    summary?.overallRisk ?? "UNKNOWN";

  const vulnerabilityCount =
    summary?.vulnerabilityCount ??
    vulnerabilities.length;

  const affectedComponentCount =
    summary?.affectedComponentCount ?? 0;


  /*
   * =====================================================
   * DEPENDENCIES
   * =====================================================
   */

  const dependencyCount =
    dependencies.length;

  const directDependencyCount =
    dependencies.filter(
      dependency =>
        dependency.direct === true
    ).length;

  const transitiveDependencyCount =
    dependencies.filter(
      dependency =>
        dependency.direct !== true
    ).length;

  const maxDependencyDepth =
    dependencies.length > 0
      ? Math.max(
          ...dependencies.map(
            dependency =>
              dependency.depth || 0
          )
        )
      : 0;


  /*
   * =====================================================
   * SEVERITY
   * =====================================================
   */

  const criticalCount =
    vulnerabilities.filter(
      vulnerability =>
        vulnerability.severity
          ?.toUpperCase() ===
        "CRITICAL"
    ).length;

  const highCount =
    vulnerabilities.filter(
      vulnerability =>
        vulnerability.severity
          ?.toUpperCase() ===
        "HIGH"
    ).length;

  const mediumCount =
    vulnerabilities.filter(
      vulnerability =>
        vulnerability.severity
          ?.toUpperCase() ===
        "MEDIUM"
    ).length;

  const lowCount =
    vulnerabilities.filter(
      vulnerability =>
        vulnerability.severity
          ?.toUpperCase() ===
        "LOW"
    ).length;


  /*
   * =====================================================
   * UI
   * =====================================================
   */

  return (
    <main className="dashboard-page">


      {/* HEADER */}

      <section className="page-heading">

        <div>

          <div className="eyebrow">
            SECURITY DASHBOARD
          </div>

          <h1>
            Security Posture
          </h1>

          <p>
            Dependency intelligence and
            supply-chain risk analysis
            for your project.
          </p>

        </div>


        <div
          className={`backend-status ${
            backendHealthy
              ? "online"
              : "offline"
          }`}
        >

          <span className="status-dot" />

          {backendHealthy
            ? "Backend connected"
            : "Backend unavailable"}

        </div>

      </section>


      {/* PROJECT */}

      <section className="project-banner">

        <div className="project-info">

          <div className="project-icon">
            GH
          </div>

          <div>

            <div className="eyebrow">
              PROJECT
            </div>

            <h2>
              OpenChain Sentinel
            </h2>

            <div className="project-meta">

              <span>
                Project {projectId.substring(0, 8)}
              </span>

              <span>•</span>

              <span>
                Scan {scanId.substring(0, 8)}
              </span>

            </div>

          </div>

        </div>

      </section>


      {/* MAIN */}

      <div className="dashboard-main">


        {/* RISK */}

        <RiskScoreCard
          score={riskScore}
          risk={overallRisk}
        />


        {/* STATS */}

        <div className="stat-grid">

          <StatCard
            label="Vulnerabilities"
            value={vulnerabilityCount}
            description="Known security findings"
            icon={ShieldAlert}
            accent="red"
          />

          <StatCard
            label="Dependencies"
            value={dependencyCount}
            description={`${directDependencyCount} direct · ${transitiveDependencyCount} transitive`}
            icon={Package}
            accent="blue"
          />

          <StatCard
            label="Affected Components"
            value={affectedComponentCount}
            description="Potentially impacted"
            icon={Boxes}
            accent="yellow"
          />

          <StatCard
            label="Max Dependency Depth"
            value={maxDependencyDepth}
            description="Deepest dependency path"
            icon={GitBranch}
            accent="purple"
          />

        </div>


        {/* ANALYTICS */}

        <section className="two-column-grid">

          <SeverityChart
            critical={criticalCount}
            high={highCount}
            medium={mediumCount}
            low={lowCount}
          />


          {/* DEPENDENCY HEALTH */}

          <div className="panel dependency-health">

            <div className="panel-header">

              <div>

                <div className="eyebrow">
                  DEPENDENCY HEALTH
                </div>

                <h3>
                  Dependency structure
                </h3>

              </div>

              <span className="panel-count">
                {dependencyCount} packages
              </span>

            </div>


            <div className="dependency-health-main">

              <div className="dependency-total">
                {dependencyCount}
              </div>

              <div>

                <strong>
                  Total dependencies
                </strong>

                <p>
                  Resolved from the Maven
                  dependency tree.
                </p>

              </div>

            </div>


            <div className="dependency-health-grid">

              <div>

                <span>
                  Direct
                </span>

                <strong>
                  {directDependencyCount}
                </strong>

              </div>

              <div>

                <span>
                  Transitive
                </span>

                <strong>
                  {transitiveDependencyCount}
                </strong>

              </div>

              <div>

                <span>
                  Max depth
                </span>

                <strong>
                  {maxDependencyDepth}
                </strong>

              </div>

            </div>

          </div>

        </section>


        {/* VULNERABILITIES */}

        <VulnerabilityTable
          vulnerabilities={
            vulnerabilities
          }
          riskAssessments={
            riskAssessments
          }
          onSelect={
            handleSelectVulnerability
          }
        />

      </div>


      {/* DRAWER */}

      {selectedVulnerability && (
        <VulnerabilityDrawer
          vulnerability={
            selectedVulnerability
          }
          risk={selectedRisk}
          onClose={
            handleCloseDrawer
          }
        />
      )}

    </main>
  );
}

export default Dashboard;