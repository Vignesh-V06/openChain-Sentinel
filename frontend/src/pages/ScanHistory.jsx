import {
  useEffect,
  useState,
} from "react";

import {
  Link,
  useParams,
} from "react-router-dom";

import {
  History,
  ExternalLink,
  CheckCircle2,
  Loader2,
  Clock3,
  XCircle,
  ShieldAlert,
} from "lucide-react";

import {
  getProjects,
  getProject,
  getScans,
} from "../services/api";


function ScanHistory() {

  const {
    projectId,
  } = useParams();

  const [scans, setScans] =
    useState([]);

  const [projectNames, setProjectNames] =
    useState({});

  const [project, setProject] =
    useState(null);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState(null);


  useEffect(() => {

    const loadHistory =
      async () => {

        try {

          setLoading(true);

          setError(null);


          /*
           * PROJECT-SPECIFIC HISTORY
           */

          if (projectId) {

            const [
              projectData,
              scanData,
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
              Array.isArray(scanData)
                ? scanData.map(
                    scan => ({
                      ...scan,
                      projectId,
                    })
                  )
                : []
            );

            setProjectNames({
              [projectId]:
                projectData.name,
            });

            return;
          }


          /*
           * GLOBAL HISTORY
           */

          const projects =
            await getProjects();


          if (
            !Array.isArray(projects) ||
            projects.length === 0
          ) {

            setScans([]);

            return;

          }


          const results =
            await Promise.all(
              projects.map(
                async projectItem => {

                  try {

                    const scanData =
                      await getScans(
                        projectItem.id
                      );

                    return {
                      project:
                        projectItem,

                      scans:
                        Array.isArray(
                          scanData
                        )
                          ? scanData
                          : [],
                    };

                  } catch {

                    return {
                      project:
                        projectItem,

                      scans: [],
                    };

                  }

                }
              )
            );


          const names = {};

          const combined = [];

          results.forEach(
            result => {

              names[
                result.project.id
              ] =
                result.project.name;


              result.scans.forEach(
                scan => {

                  combined.push({
                    ...scan,
                    projectId:
                      result.project.id,
                  });

                }
              );

            }
          );


          combined.sort(
            (a, b) => {

              const dateA =
                new Date(
                  a.createdAt || 0
                );

              const dateB =
                new Date(
                  b.createdAt || 0
                );

              return dateB - dateA;

            }
          );


          setProjectNames(
            names
          );

          setScans(
            combined
          );

        } catch (err) {

          console.error(
            "Failed to load scan history:",
            err
          );

          setError(
            err?.response?.data?.message ||
            err?.message ||
            "Failed to load scan history."
          );

        } finally {

          setLoading(false);

        }

      };


    loadHistory();

  }, [projectId]);


  if (loading) {

    return (
      <main className="dashboard-page">

        <div className="page-loading">

          <div className="loading-spinner" />

          <p>
            Loading scan history...
          </p>

        </div>

      </main>
    );

  }


  return (
    <main className="dashboard-page">

      {/* HEADER */}

      <section className="page-heading">

        <div>

          <div className="eyebrow">
            SCANS
          </div>

          <h1>
            {project
              ? `${project.name} scans`
              : "Scan history"}
          </h1>

          <p>
            Review previous dependency and
            security analysis runs.
          </p>

        </div>

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


      {/* EMPTY */}

      {scans.length === 0 ? (

        <div className="panel">

          <div className="empty-projects">

            <div className="empty-project-icon">

              <History size={25} />

            </div>

            <h3>
              No scans yet
            </h3>

            <p>
              Once a project is scanned,
              its execution history will
              appear here.
            </p>

          </div>

        </div>

      ) : (

        <div className="panel">

          <div className="panel-header">

            <div>

              <div className="eyebrow">
                SCAN HISTORY
              </div>

              <h3>
                Previous scans
              </h3>

            </div>

            <span className="panel-count">
              {scans.length} scans
            </span>

          </div>


          <div className="scan-list">

            {scans.map(
              scan => (

                <Link
                  key={scan.id}
                  to={`/projects/${scan.projectId}/scans/${scan.id}`}
                  className="scan-row"
                >

                  <div className="scan-row-main">

                    <div className="scan-icon">
                      <History
                        size={15}
                      />
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
                        {projectNames[
                          scan.projectId
                        ] || "Project"}

                        {" • "}

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

        </div>

      )}

    </main>
  );
}


/* =========================================================
   STATUS
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


export default ScanHistory;