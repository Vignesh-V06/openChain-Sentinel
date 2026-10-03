import {
  useEffect,
  useState,
} from "react";

import {
  Link,
  useNavigate,
  useParams,
} from "react-router-dom";

import {
  ArrowLeft,
  ExternalLink,
  Play,
  History,
  ShieldAlert,
  CheckCircle2,
  Loader2,
  Clock3,
  XCircle,
} from "lucide-react";

import {
  getProject,
  getScans,
  createScan,
} from "../services/api";


function ProjectDetails() {

  const {
    projectId,
  } = useParams();

  const navigate =
    useNavigate();

  const [project, setProject] =
    useState(null);

  const [scans, setScans] =
    useState([]);

  const [loading, setLoading] =
    useState(true);

  const [scanning, setScanning] =
    useState(false);

  const [error, setError] =
    useState(null);


  const loadData =
    async () => {

      try {

        setLoading(true);

        setError(null);

        const [
          projectData,
          scansData,
        ] = await Promise.all([

          getProject(
            projectId
          ),

          getScans(
            projectId
          ),

        ]);

        setProject(
          projectData
        );

        setScans(
          Array.isArray(scansData)
            ? [...scansData].sort(
                (a, b) =>
                  new Date(
                    b.createdAt || 0
                  ) -
                  new Date(
                    a.createdAt || 0
                  )
              )
            : []
        );

      } catch (err) {

        console.error(
          "Failed to load project:",
          err
        );

        setError(
          err?.response?.data?.message ||
          err?.message ||
          "Failed to load project."
        );

      } finally {

        setLoading(false);

      }

    };


  useEffect(() => {

    loadData();

  }, [projectId]);


  /*
   * =====================================================
   * START SCAN
   * =====================================================
   */

  const handleScan =
    async () => {

      try {

        setScanning(true);

        setError(null);


        const scan =
          await createScan(
            projectId
          );


        /*
         * The backend now returns
         * immediately with PENDING.
         *
         * ScanView takes over from here.
         */

        navigate(
          `/projects/${projectId}/scans/${scan.id}`
        );

      } catch (err) {

        console.error(
          "Failed to start scan:",
          err
        );

        setError(
          err?.response?.data?.message ||
          err?.message ||
          "Failed to start scan."
        );

        setScanning(false);

      }

    };


  if (loading) {

    return (
      <main className="dashboard-page">

        <div className="page-loading">

          <div className="loading-spinner" />

          <p>
            Loading project...
          </p>

        </div>

      </main>
    );

  }


  if (
    error &&
    !project
  ) {

    return (
      <main className="dashboard-page">

        <div className="page-error">

          <ShieldAlert size={28} />

          <h2>
            Unable to load project
          </h2>

          <p>
            {error}
          </p>

        </div>

      </main>
    );

  }


  return (
    <main className="dashboard-page">

      {/* BACK */}

      <div className="project-details-back">

        <Link
          to="/projects"
          className="back-link"
        >
          <ArrowLeft size={14} />

          Back to projects
        </Link>

      </div>


      {/* HEADER */}

      <section className="project-details-header">

        <div>

          <div className="eyebrow">
            PROJECT
          </div>

          <h1>
            {project?.name}
          </h1>

          <a
            href={
              project?.repositoryUrl
            }
            target="_blank"
            rel="noreferrer"
            className="repository-link"
          >

            {project?.repositoryUrl}

            <ExternalLink
              size={13}
            />

          </a>

        </div>


        <button
          type="button"
          className="primary-button"
          onClick={handleScan}
          disabled={scanning}
        >

          {scanning ? (
            <Loader2
              size={14}
              className="spin"
            />
          ) : (
            <Play size={14} />
          )}

          {scanning
            ? "Starting..."
            : "Scan now"}

        </button>

      </section>


      {/* ERROR */}

      {error && (

        <div className="project-error">

          <span className="error-icon">
            !
          </span>

          {error}

        </div>

      )}


      {/* SCANS */}

      <section className="panel">

        <div className="panel-header">

          <div>

            <div className="eyebrow">
              SCAN HISTORY
            </div>

            <h3>
              Project scans
            </h3>

          </div>


          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "12px",
            }}
          >

            <span className="panel-count">
              {scans.length} scans
            </span>

            {scans.length > 0 && (

              <Link
                to={`/projects/${projectId}/scans`}
                className="project-open-button"
              >
                View all
              </Link>

            )}

          </div>

        </div>


        {scans.length === 0 ? (

          <div className="empty-projects">

            <div className="empty-project-icon">

              <History size={25} />

            </div>

            <h3>
              No scans yet
            </h3>

            <p>
              Run the first scan to resolve
              dependencies and analyze
              vulnerabilities.
            </p>

            <button
              type="button"
              className="primary-button"
              onClick={handleScan}
              disabled={scanning}
            >

              <Play size={14} />

              {scanning
                ? "Starting..."
                : "Run first scan"}

            </button>

          </div>

        ) : (

          <div className="scan-list">

            {scans.slice(0, 5).map(
              scan => (

                <Link
                  key={scan.id}
                  to={`/projects/${projectId}/scans/${scan.id}`}
                  className="scan-row"
                >

                  <div className="scan-row-main">

                    <div className="scan-icon">
                      <History size={15} />
                    </div>


                    <div>

                      <strong>
                        Scan{" "}
                        {scan.id.substring(
                          0,
                          8
                        )}
                      </strong>

                      <span>
                        {scan.createdAt
                          ? new Date(
                              scan.createdAt
                            ).toLocaleString()
                          : "Unknown time"}
                      </span>

                    </div>

                  </div>


                  <div className="scan-row-right">

                    <ScanStatus
                      status={
                        scan.status
                      }
                    />

                    <ExternalLink
                      size={14}
                    />

                  </div>

                </Link>

              )
            )}

          </div>

        )}

      </section>

    </main>
  );
}


/* =========================================================
   SCAN STATUS
========================================================= */

function ScanStatus({
  status,
}) {

  const normalized =
    String(
      status || "UNKNOWN"
    ).toUpperCase();


  if (
    normalized === "COMPLETED"
  ) {

    return (
      <span className="scan-status-badge completed">

        <CheckCircle2 size={11} />

        COMPLETED

      </span>
    );

  }


  if (
    normalized === "RUNNING"
  ) {

    return (
      <span className="scan-status-badge running">

        <Loader2
          size={11}
          className="spin"
        />

        RUNNING

      </span>
    );

  }


  if (
    normalized === "FAILED"
  ) {

    return (
      <span className="scan-status-badge failed">

        <XCircle size={11} />

        FAILED

      </span>
    );

  }


  return (
    <span className="scan-status-badge pending">

      <Clock3 size={11} />

      PENDING

    </span>
  );
}


export default ProjectDetails;