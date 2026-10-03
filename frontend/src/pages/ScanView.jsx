import {
  useEffect,
  useState,
} from "react";

import {
  useParams,
  useNavigate,
} from "react-router-dom";

import {
  CheckCircle2,
  Circle,
  Clock3,
  Loader2,
  XCircle,
  ArrowLeft,
} from "lucide-react";

import {
  getScan,
} from "../services/api";

import Dashboard from "./Dashboard";


function ScanView() {

  const {
    projectId,
    scanId,
  } = useParams();

  const navigate =
    useNavigate();

  const [scan, setScan] =
    useState(null);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState(null);


  /*
   * =====================================================
   * LOAD SCAN
   * =====================================================
   */

  useEffect(() => {

    let intervalId;

    const loadScan =
      async () => {

        try {

          const data =
            await getScan(
              projectId,
              scanId
            );

          setScan(data);

          setLoading(false);


          /*
           * Stop polling when the
           * scan reaches a terminal state.
           */

          if (
            data.status === "COMPLETED" ||
            data.status === "FAILED"
          ) {

            if (intervalId) {
              clearInterval(intervalId);
            }

          }

        } catch (err) {

          console.error(
            "Failed to load scan:",
            err
          );

          setError(
            err?.response?.data?.message ||
            err?.message ||
            "Unable to load scan."
          );

          setLoading(false);

        }
      };


    loadScan();


    /*
     * Poll while the scan is active.
     */

    intervalId =
      setInterval(
        loadScan,
        2000
      );


    return () => {

      if (intervalId) {
        clearInterval(intervalId);
      }

    };

  }, [
    projectId,
    scanId,
  ]);


  /*
   * =====================================================
   * LOADING
   * =====================================================
   */

  if (loading) {

    return (
      <main className="dashboard-page">

        <div className="page-loading">

          <Loader2
            size={26}
            className="scan-loading-icon"
          />

          <p>
            Loading scan...
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

          <XCircle size={30} />

          <h2>
            Unable to load scan
          </h2>

          <p>
            {error}
          </p>

          <button
            className="secondary-button"
            onClick={() =>
              navigate(
                `/projects/${projectId}`
              )
            }
          >
            <ArrowLeft size={14} />

            Back to project
          </button>

        </div>

      </main>
    );

  }


  /*
   * =====================================================
   * COMPLETED
   * =====================================================
   */

  if (
    scan?.status === "COMPLETED"
  ) {

    return (
      <Dashboard />
    );

  }


  /*
   * =====================================================
   * FAILED
   * =====================================================
   */

  if (
    scan?.status === "FAILED"
  ) {

    return (
      <ScanFailed
        projectId={projectId}
      />
    );

  }


  /*
   * =====================================================
   * ACTIVE SCAN
   * =====================================================
   */

  return (
    <ScanProgress
      scan={scan}
    />
  );
}


/* =========================================================
   SCAN PROGRESS
========================================================= */

function ScanProgress({ scan }) {

  const status =
    scan?.status || "PENDING";

  const running =
    status === "RUNNING";

  const steps = [
    {
      label: "Scan created",
      description:
        "Scan request registered.",
      complete:
        status === "RUNNING" ||
        status === "COMPLETED",
    },

    {
      label: "Dependency analysis",
      description:
        "Resolving Maven dependencies.",
      complete:
        status === "RUNNING" ||
        status === "COMPLETED",
    },

    {
      label: "Security analysis",
      description:
        "Checking dependency vulnerabilities.",
      complete:
        status === "RUNNING" ||
        status === "COMPLETED",
    },

    {
      label: "Risk analysis",
      description:
        "Calculating contextual project risk.",
      complete:
        status === "COMPLETED",
    },

    {
      label: "Scan completed",
      description:
        "Security results are ready.",
      complete:
        status === "COMPLETED",
    },
  ];


  return (
    <main className="dashboard-page">

      <section className="scan-progress-page">

        <div className="scan-progress-header">

          <div>

            <div className="eyebrow">
              SECURITY SCAN
            </div>

            <h1>
              Scan in progress
            </h1>

            <p>
              OpenChain Sentinel is analyzing
              the project's dependency supply chain.
            </p>

          </div>


          <div
            className={`scan-live-status ${
              running
                ? "running"
                : "pending"
            }`}
          >

            {running ? (
              <Loader2
                size={14}
                className="spin"
              />
            ) : (
              <Clock3 size={14} />
            )}

            {status}

          </div>

        </div>


        <div className="scan-progress-card">

          <div className="scan-progress-title">

            <div>

              <span className="eyebrow">
                SCAN ID
              </span>

              <strong>
                {scan?.id}
              </strong>

            </div>

            <span>
              {scan?.createdAt
                ? new Date(
                    scan.createdAt
                  ).toLocaleString()
                : ""}
            </span>

          </div>


          <div className="scan-progress-list">

            {steps.map(
              (step, index) => {

                const isCurrent =
                  !step.complete &&
                  (
                    index === 0 ||
                    steps[index - 1]
                      .complete
                  );

                return (
                  <div
                    className={`scan-step ${
                      step.complete
                        ? "complete"
                        : isCurrent
                        ? "current"
                        : ""
                    }`}
                    key={step.label}
                  >

                    <div className="scan-step-icon">

                      {step.complete ? (
                        <CheckCircle2
                          size={19}
                        />
                      ) : isCurrent ? (
                        <Loader2
                          size={19}
                          className="spin"
                        />
                      ) : (
                        <Circle
                          size={19}
                        />
                      )}

                    </div>


                    <div>

                      <strong>
                        {step.label}
                      </strong>

                      <p>
                        {step.description}
                      </p>

                    </div>

                  </div>
                );

              }
            )}

          </div>


          <div className="scan-progress-footer">

            <Loader2
              size={14}
              className="spin"
            />

            <span>
              This page updates automatically.
            </span>

          </div>

        </div>

      </section>

    </main>
  );
}


/* =========================================================
   FAILED
========================================================= */

function ScanFailed({
  projectId,
}) {

  return (
    <main className="dashboard-page">

      <section className="scan-progress-page">

        <div className="scan-failed-card">

          <div className="scan-failed-icon">
            <XCircle size={30} />
          </div>

          <div>

            <div className="eyebrow">
              SECURITY SCAN
            </div>

            <h1>
              Scan failed
            </h1>

            <p>
              The scan could not be completed.
              Check the backend logs for the
              underlying error.
            </p>

            <button
              className="secondary-button"
              onClick={() =>
                window.location.href =
                  `/projects/${projectId}`
              }
            >
              <ArrowLeft size={14} />

              Back to project
            </button>

          </div>

        </div>

      </section>

    </main>
  );
}


export default ScanView;