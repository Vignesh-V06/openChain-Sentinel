import {
  useEffect,
  useState,
} from "react";

import {
  Link,
} from "react-router-dom";

import {
  FolderGit2,
  Plus,
  ExternalLink,
  Trash2,
  GitBranch,
  X,
} from "lucide-react";

import {
  getProjects,
  createProject,
  deleteProject,
} from "../services/api";


function Projects() {

  const [projects, setProjects] =
    useState([]);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState(null);

  const [showCreate, setShowCreate] =
    useState(false);

  const [name, setName] =
    useState("");

  const [repositoryUrl, setRepositoryUrl] =
    useState("");

  const [creating, setCreating] =
    useState(false);


  const loadProjects =
    async () => {

      try {

        setLoading(true);

        setError(null);

        const data =
          await getProjects();

        setProjects(
          Array.isArray(data)
            ? data
            : []
        );

      } catch (err) {

        console.error(
          "Failed to load projects:",
          err
        );

        setError(
          err?.response?.data?.message ||
          err?.message ||
          "Failed to load projects."
        );

      } finally {

        setLoading(false);

      }
    };


  useEffect(() => {

    loadProjects();

  }, []);


  const handleCreate =
    async (event) => {

      event.preventDefault();

      if (
        !name.trim() ||
        !repositoryUrl.trim()
      ) {
        return;
      }

      try {

        setCreating(true);

        const project =
          await createProject({
            name: name.trim(),
            repositoryUrl:
              repositoryUrl.trim(),
          });

        setProjects(
          previous => [
            ...previous,
            project,
          ]
        );

        setName("");

        setRepositoryUrl("");

        setShowCreate(false);

      } catch (err) {

        console.error(
          "Failed to create project:",
          err
        );

        setError(
          err?.response?.data?.message ||
          err?.message ||
          "Failed to create project."
        );

      } finally {

        setCreating(false);

      }
    };


  const handleDelete =
    async (projectId) => {

      const confirmed =
        window.confirm(
          "Delete this project?"
        );

      if (!confirmed) {
        return;
      }

      try {

        await deleteProject(
          projectId
        );

        setProjects(
          previous =>
            previous.filter(
              project =>
                project.id !== projectId
            )
        );

      } catch (err) {

        console.error(
          "Failed to delete project:",
          err
        );

        setError(
          err?.response?.data?.message ||
          err?.message ||
          "Failed to delete project."
        );
      }
    };


  if (loading) {

    return (
      <main className="dashboard-page">

        <div className="page-loading">

          <div className="loading-spinner" />

          <p>
            Loading projects...
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
            PROJECTS
          </div>

          <h1>
            Your projects
          </h1>

          <p>
            Manage repositories and inspect
            their supply-chain security posture.
          </p>

        </div>


        <button
          type="button"
          className="primary-button"
          onClick={() =>
            setShowCreate(true)
          }
        >
          <Plus size={15} />

          New project
        </button>

      </section>


      {/* ERROR */}

      {error && (

        <div className="project-error">

          <ShieldAlertIcon />

          <span>
            {error}
          </span>

        </div>

      )}


      {/* PROJECTS */}

      {projects.length === 0 ? (

        <div className="panel">

          <div className="empty-projects">

            <div className="empty-project-icon">
              <FolderGit2 size={26} />
            </div>

            <h3>
              No projects yet
            </h3>

            <p>
              Add a public GitHub Maven
              repository to begin analyzing
              its dependency supply chain.
            </p>

            <button
              type="button"
              className="primary-button"
              onClick={() =>
                setShowCreate(true)
              }
            >
              <Plus size={15} />

              Add your first project
            </button>

          </div>

        </div>

      ) : (

        <div className="projects-grid">

          {projects.map(
            project => (

              <article
                className="project-card"
                key={project.id}
              >

                <div className="project-card-header">

                  <div className="project-card-icon">
                    <GitBranch size={17} />
                  </div>


                  <button
                    type="button"
                    className="project-delete"
                    onClick={() =>
                      handleDelete(
                        project.id
                      )
                    }
                    title="Delete project"
                  >
                    <Trash2 size={14} />
                  </button>

                </div>


                <div className="project-card-body">

                  <div className="eyebrow">
                    PROJECT
                  </div>

                  <h3>
                    {project.name}
                  </h3>

                  <p>
                    {project.repositoryUrl}
                  </p>

                </div>


                <div className="project-card-footer">

                  <Link
                    to={`/projects/${project.id}`}
                    className="project-open-button"
                  >
                    <span>
                      Open project
                    </span>

                    <ExternalLink size={13} />
                  </Link>

                </div>

              </article>

            )
          )}

        </div>

      )}


      {/* CREATE MODAL */}

      {showCreate && (

        <div
          className="modal-backdrop"
          onMouseDown={() =>
            setShowCreate(false)
          }
        >

          <div
            className="create-project-modal"
            onMouseDown={event =>
              event.stopPropagation()
            }
          >

            <div className="create-modal-header">

              <div>

                <div className="eyebrow">
                  NEW PROJECT
                </div>

                <h2>
                  Add repository
                </h2>

              </div>


              <button
                type="button"
                className="modal-close"
                onClick={() =>
                  setShowCreate(false)
                }
              >
                <X size={16} />
              </button>

            </div>


            <form
              onSubmit={handleCreate}
            >

              <div className="form-field">

                <label>
                  Project name
                </label>

                <input
                  type="text"
                  value={name}
                  onChange={event =>
                    setName(
                      event.target.value
                    )
                  }
                  placeholder="OpenChain Sentinel"
                  required
                />

              </div>


              <div className="form-field">

                <label>
                  GitHub repository URL
                </label>

                <input
                  type="url"
                  value={repositoryUrl}
                  onChange={event =>
                    setRepositoryUrl(
                      event.target.value
                    )
                  }
                  placeholder="https://github.com/user/repository"
                  required
                />

              </div>


              <div className="create-modal-actions">

                <button
                  type="button"
                  className="secondary-button"
                  onClick={() =>
                    setShowCreate(false)
                  }
                >
                  Cancel
                </button>


                <button
                  type="submit"
                  className="primary-button"
                  disabled={creating}
                >
                  {creating
                    ? "Creating..."
                    : "Create project"}
                </button>

              </div>

            </form>

          </div>

        </div>

      )}

    </main>
  );
}


/*
 * Small local icon wrapper so
 * the error UI does not need
 * another dependency.
 */

function ShieldAlertIcon() {
  return (
    <span className="error-icon">
      !
    </span>
  );
}


export default Projects;