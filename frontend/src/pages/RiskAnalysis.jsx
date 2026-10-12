import { useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  ShieldCheck,
  Target,
  TrendingUp,
} from "lucide-react";
import { useNavigate } from "react-router-dom";

import {
  getProjects,
  getScans,
  getRiskAssessments,
  getScanSummary,
} from "../services/api";

function RiskAnalysis() {
  const navigate = useNavigate();

  const [projects, setProjects] = useState([]);
  const [selectedProject, setSelectedProject] = useState(null);

  const [scans, setScans] = useState([]);
  const [selectedScan, setSelectedScan] = useState(null);

  const [risks, setRisks] = useState([]);
  const [summary, setSummary] = useState(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  /*
   * --------------------------------------------------
   * LOAD PROJECTS
   * --------------------------------------------------
   */

  useEffect(() => {
    const loadProjects = async () => {
      try {
        setLoading(true);

        const data = await getProjects();

        const projectList =
          Array.isArray(data) ? data : [];

        setProjects(projectList);

        if (projectList.length > 0) {
          setSelectedProject(projectList[0]);
        }
      } catch (err) {
        console.error(err);
        setError("Unable to load projects.");
      } finally {
        setLoading(false);
      }
    };

    loadProjects();
  }, []);

  /*
   * --------------------------------------------------
   * LOAD SCANS
   * --------------------------------------------------
   */

  useEffect(() => {
    if (!selectedProject?.id) {
      return;
    }

    const loadScans = async () => {
      try {
        setLoading(true);

        const data =
          await getScans(selectedProject.id);

        const scanList =
          Array.isArray(data) ? data : [];

        setScans(scanList);

        const completed =
          scanList
            .filter(
              (scan) =>
                scan.status === "COMPLETED"
            )
            .sort(
              (a, b) =>
                new Date(b.createdAt || 0) -
                new Date(a.createdAt || 0)
            );

        if (completed.length > 0) {
          setSelectedScan(completed[0]);
        } else if (scanList.length > 0) {
          setSelectedScan(scanList[0]);
        }
      } catch (err) {
        console.error(err);
        setError(
          "Unable to load scan history."
        );
      } finally {
        setLoading(false);
      }
    };

    loadScans();
  }, [selectedProject]);

  /*
   * --------------------------------------------------
   * LOAD RISK DATA
   * --------------------------------------------------
   */

  useEffect(() => {
    if (
      !selectedProject?.id ||
      !selectedScan?.id
    ) {
      return;
    }

    const loadRiskData = async () => {
      try {
        setLoading(true);
        setError("");

        const [
          riskResponse,
          summaryResponse,
        ] = await Promise.all([
          getRiskAssessments(
            selectedProject.id,
            selectedScan.id
          ),
          getScanSummary(
            selectedProject.id,
            selectedScan.id
          ),
        ]);

        setRisks(
          Array.isArray(riskResponse)
            ? riskResponse
            : []
        );

        setSummary(summaryResponse || null);
      } catch (err) {
        console.error(err);
        setError(
          "Unable to load risk analysis."
        );
      } finally {
        setLoading(false);
      }
    };

    loadRiskData();
  }, [selectedProject, selectedScan]);

  /*
   * --------------------------------------------------
   * RISK COUNTS
   * --------------------------------------------------
   */

  const priorityCounts = useMemo(() => {
    return {
      p1: risks.filter(
        (risk) => risk.priority === "P1"
      ).length,

      p2: risks.filter(
        (risk) => risk.priority === "P2"
      ).length,

      p3: risks.filter(
        (risk) => risk.priority === "P3"
      ).length,
    };
  }, [risks]);

  /*
   * --------------------------------------------------
   * SCORE COLOR
   * --------------------------------------------------
   */

  const getRiskClass = (level) => {
    switch (level?.toUpperCase()) {
      case "CRITICAL":
        return "critical";

      case "HIGH":
        return "high";

      case "MEDIUM":
        return "medium";

      case "LOW":
        return "low";

      default:
        return "unknown";
    }
  };

  /*
   * --------------------------------------------------
   * EMPTY PROJECT STATE
   * --------------------------------------------------
   */

  if (!loading && projects.length === 0) {
    return (
      <main className="page-shell">

        <section className="page-header">

          <div>
            <div className="eyebrow">
              SECURITY ANALYSIS
            </div>

            <h1>Risk Analysis</h1>

            <p>
              Understand why vulnerabilities receive
              their project-specific risk priority.
            </p>
          </div>

        </section>

        <div className="empty-page-card">

          <Target size={34} />

          <h3>No projects available</h3>

          <p>
            Create a project and run a scan to
            generate risk assessments.
          </p>

          <button
            className="primary-button"
            onClick={() =>
              navigate("/projects")
            }
          >
            Go to Projects
          </button>

        </div>

      </main>
    );
  }

  return (
    <main className="page-shell">

      {/* -------------------------------------------- */}
      {/* HEADER */}
      {/* -------------------------------------------- */}

      <section className="page-header">

        <div>
          <div className="eyebrow">
            SECURITY ANALYSIS
          </div>

          <h1>Risk Analysis</h1>

          <p>
            Contextual prioritization of vulnerability
            findings across your dependency graph.
          </p>
        </div>

        <div className="page-header-actions">

          <select
            className="page-select"
            value={selectedProject?.id || ""}
            onChange={(event) => {
              const project =
                projects.find(
                  (item) =>
                    item.id ===
                    event.target.value
                );

              setSelectedProject(
                project || null
              );
            }}
          >

            {projects.map((project) => (
              <option
                key={project.id}
                value={project.id}
              >
                {project.name}
              </option>
            ))}

          </select>

          <select
            className="page-select"
            value={selectedScan?.id || ""}
            onChange={(event) => {
              const scan =
                scans.find(
                  (item) =>
                    item.id ===
                    event.target.value
                );

              setSelectedScan(
                scan || null
              );
            }}
          >

            {scans.map((scan) => (
              <option
                key={scan.id}
                value={scan.id}
              >
                Scan{" "}
                {scan.id.substring(0, 8)}
              </option>
            ))}

          </select>

        </div>

      </section>

      {error && (
        <div className="page-error">
          {error}
        </div>
      )}

      {/* -------------------------------------------- */}
      {/* HERO */}
      {/* -------------------------------------------- */}

      <section className="risk-hero-grid">

        <div className="risk-overview-card panel">

          <div className="risk-overview-top">

            <div>

              <div className="eyebrow">
                OVERALL PROJECT RISK
              </div>

              <h2>
                {summary?.overallRisk ||
                  "UNKNOWN"}
              </h2>

            </div>

            <div
              className={`risk-shield ${getRiskClass(
                summary?.overallRisk
              )}`}
            >
              <ShieldCheck size={28} />
            </div>

          </div>

          <div className="risk-score-large">

            <strong>
              {summary?.highestRiskScore ??
                0}
            </strong>

            <span>/ 100</span>

          </div>

          <p>
            Highest risk score identified in
            the selected scan.
          </p>

        </div>

        <div className="risk-stat-stack">

          <div className="risk-mini-card">

            <div className="risk-mini-icon red">
              <AlertTriangle size={18} />
            </div>

            <div>
              <span>Total findings</span>
              <strong>
                {summary?.vulnerabilityCount ??
                  risks.length}
              </strong>
            </div>

          </div>

          <div className="risk-mini-card">

            <div className="risk-mini-icon purple">
              <Target size={18} />
            </div>

            <div>
              <span>P1 findings</span>
              <strong>
                {priorityCounts.p1}
              </strong>
            </div>

          </div>

          <div className="risk-mini-card">

            <div className="risk-mini-icon blue">
              <TrendingUp size={18} />
            </div>

            <div>
              <span>Affected components</span>
              <strong>
                {summary?.affectedComponentCount ??
                  0}
              </strong>
            </div>

          </div>

        </div>

      </section>

      {/* -------------------------------------------- */}
      {/* PRIORITY DISTRIBUTION */}
      {/* -------------------------------------------- */}

      <section className="panel risk-priority-panel">

        <div className="panel-header">

          <div>
            <div className="eyebrow">
              PRIORITY DISTRIBUTION
            </div>

            <h3>
              Where should you start?
            </h3>

            <p>
              Findings ordered by the project's
              contextual risk assessment.
            </p>
          </div>

        </div>

        <div className="priority-grid">

          <div className="priority-card p1">
            <span>P1</span>
            <strong>
              {priorityCounts.p1}
            </strong>
            <small>Immediate attention</small>
          </div>

          <div className="priority-card p2">
            <span>P2</span>
            <strong>
              {priorityCounts.p2}
            </strong>
            <small>High attention</small>
          </div>

          <div className="priority-card p3">
            <span>P3</span>
            <strong>
              {priorityCounts.p3}
            </strong>
            <small>Lower priority</small>
          </div>

        </div>

      </section>

      {/* -------------------------------------------- */}
      {/* FINDINGS */}
      {/* -------------------------------------------- */}

      <section className="panel risk-findings-panel">

        <div className="panel-header">

          <div>
            <div className="eyebrow">
              RISK FINDINGS
            </div>

            <h3>
              Contextual risk assessments
            </h3>

            <p>
              Risk is based on vulnerability
              severity and dependency context.
            </p>
          </div>

          <span className="finding-count">
            {risks.length} findings
          </span>

        </div>

        <div className="risk-findings-list">

          {loading ? (
            <div className="page-loading">
              Loading risk analysis...
            </div>
          ) : risks.length === 0 ? (
            <div className="empty-inline">
              <ShieldCheck size={28} />

              <span>
                No risk findings for this scan.
              </span>
            </div>
          ) : (
            [...risks]
              .sort(
                (a, b) =>
                  (b.riskScore || 0) -
                  (a.riskScore || 0)
              )
              .map((risk) => (

                <div
                  className="risk-finding-row"
                  key={
                    risk.id ||
                    risk.vulnerabilityId
                  }
                >

                  <div className="risk-score-cell">

                    <strong>
                      {risk.riskScore ?? "—"}
                    </strong>

                    <span>
                      / 100
                    </span>

                  </div>

                  <div className="risk-finding-main">

                    <div className="risk-finding-title">

                      <strong>
                        {risk.vulnerabilityId}
                      </strong>

                      <span
                        className={`risk-level-badge ${getRiskClass(
                          risk.riskLevel
                        )}`}
                      >
                        {risk.riskLevel ||
                          "UNKNOWN"}
                      </span>

                    </div>

                    <div className="risk-finding-meta">

                      <span>
                        Priority{" "}
                        {risk.priority ||
                          "—"}
                      </span>

                      <span>
                        Depth{" "}
                        {risk.maxDependencyDepth ??
                          risk.dependencyDepth ??
                          "—"}
                      </span>

                      <span>
                        Paths{" "}
                        {risk.affectedPathCount ??
                          0}
                      </span>

                      <span>
                        Components{" "}
                        {risk.affectedComponentCount ??
                          0}
                      </span>

                    </div>

                    {Array.isArray(
                      risk.reasons
                    ) &&
                      risk.reasons.length > 0 && (
                        <div className="risk-reasons">

                          {risk.reasons
                            .slice(0, 3)
                            .map(
                              (reason, index) => (
                                <span
                                  key={index}
                                >
                                  ✓ {reason}
                                </span>
                              )
                            )}

                        </div>
                      )}

                  </div>

                  <div className="risk-action">
                    <button
                      className="ghost-button"
                      onClick={() =>
                        navigate(
                          `/vulnerabilities?vulnerability=${encodeURIComponent(
                            risk.vulnerabilityId
                          )}`
                        )
                      }
                    >
                      Investigate →
                    </button>
                  </div>

                </div>

              ))
          )}

        </div>

      </section>

    </main>
  );
}

export default RiskAnalysis;