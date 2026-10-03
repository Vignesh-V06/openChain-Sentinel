import {
  useEffect,
  useState,
} from "react";

import {
  useNavigate,
} from "react-router-dom";

import {
  getProjects,
  getScans,
} from "../services/api";

function DashboardEntry() {
  const navigate =
    useNavigate();

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState(null);

  useEffect(() => {

    const openLatestDashboard =
      async () => {

        try {

          const projects =
            await getProjects();

          if (
            !Array.isArray(projects) ||
            projects.length === 0
          ) {
            navigate(
              "/projects",
              { replace: true }
            );

            return;
          }

          /*
           * For now we use the first
           * available project.
           *
           * Later this will use the
           * user's selected project.
           */

          const project =
            projects[0];

          const scans =
            await getScans(
              project.id
            );

          if (
            !Array.isArray(scans) ||
            scans.length === 0
          ) {
            navigate(
              `/projects/${project.id}`,
              { replace: true }
            );

            return;
          }

          /*
           * Backend returns scans in
           * creation order currently.
           *
           * We explicitly sort here so
           * "latest" is deterministic.
           */

          const sortedScans =
            [...scans].sort(
              (a, b) => {

                const dateA =
                  new Date(
                    a.createdAt || 0
                  );

                const dateB =
                  new Date(
                    b.createdAt || 0
                  );

                return (
                  dateB - dateA
                );
              }
            );

          const latestScan =
            sortedScans[0];

          navigate(
            `/projects/${project.id}/scans/${latestScan.id}`,
            { replace: true }
          );

        } catch (err) {

          console.error(
            "Failed to open dashboard:",
            err
          );

          setError(
            err?.response?.data?.message ||
            err?.message ||
            "Unable to open dashboard."
          );

        } finally {

          setLoading(false);

        }
      };

    openLatestDashboard();

  }, [navigate]);


  if (error) {

    return (
      <main className="dashboard-page">

        <div className="page-error">

          <h2>
            Unable to open dashboard
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

      <div className="page-loading">

        <div className="loading-spinner" />

        <p>
          Opening latest security dashboard...
        </p>

      </div>

    </main>
  );
}

export default DashboardEntry;