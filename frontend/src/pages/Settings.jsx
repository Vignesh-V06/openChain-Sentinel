import {
  Activity,
  Database,
  GitBranch,
  Info,
  ShieldCheck,
  Server,
} from "lucide-react";

function Settings() {
  return (
    <main className="page-shell">

      {/* -------------------------------------------- */}
      {/* HEADER */}
      {/* -------------------------------------------- */}

      <section className="page-header">

        <div>

          <div className="eyebrow">
            SYSTEM CONFIGURATION
          </div>

          <h1>Settings</h1>

          <p>
            Application configuration and service
            connectivity.
          </p>

        </div>

      </section>

      {/* -------------------------------------------- */}
      {/* APPLICATION */}
      {/* -------------------------------------------- */}

      <section className="settings-grid">

        <div className="panel settings-panel">

          <div className="settings-panel-header">

            <div className="settings-icon blue">
              <Info size={18} />
            </div>

            <div>
              <h3>Application</h3>

              <p>
                OpenChain Sentinel configuration.
              </p>
            </div>

          </div>

          <div className="settings-list">

            <div className="settings-row">
              <span>Application name</span>
              <strong>
                OpenChain Sentinel
              </strong>
            </div>

            <div className="settings-row">
              <span>Environment</span>
              <strong>Development</strong>
            </div>

            <div className="settings-row">
              <span>Frontend</span>
              <strong>React + Vite</strong>
            </div>

            <div className="settings-row">
              <span>Backend</span>
              <strong>Spring Boot</strong>
            </div>

          </div>

        </div>

        {/* ---------------------------------------- */}
        {/* DATABASES */}
        {/* ---------------------------------------- */}

        <div className="panel settings-panel">

          <div className="settings-panel-header">

            <div className="settings-icon purple">
              <Database size={18} />
            </div>

            <div>
              <h3>Data services</h3>

              <p>
                Storage systems used by the scanner.
              </p>
            </div>

          </div>

          <div className="settings-list">

            <div className="settings-row">

              <div className="settings-service">

                <Database size={16} />

                <span>MongoDB</span>

              </div>

              <span className="service-status healthy">
                ● Connected
              </span>

            </div>

            <div className="settings-row">

              <div className="settings-service">

                <GitBranch size={16} />

                <span>Neo4j</span>

              </div>

              <span className="service-status healthy">
                ● Connected
              </span>

            </div>

          </div>

        </div>

        {/* ---------------------------------------- */}
        {/* SCANNER */}
        {/* ---------------------------------------- */}

        <div className="panel settings-panel">

          <div className="settings-panel-header">

            <div className="settings-icon green">
              <ShieldCheck size={18} />
            </div>

            <div>
              <h3>Scanner</h3>

              <p>
                Current scanning configuration.
              </p>
            </div>

          </div>

          <div className="settings-list">

            <div className="settings-row">
              <span>Primary ecosystem</span>
              <strong>Java / Maven</strong>
            </div>

            <div className="settings-row">
              <span>Repository access</span>
              <strong>Public GitHub</strong>
            </div>

            <div className="settings-row">
              <span>Vulnerability source</span>
              <strong>OSV</strong>
            </div>

            <div className="settings-row">
              <span>Risk model</span>
              <strong>Rule-based</strong>
            </div>

          </div>

        </div>

        {/* ---------------------------------------- */}
        {/* SYSTEM */}
        {/* ---------------------------------------- */}

        <div className="panel settings-panel">

          <div className="settings-panel-header">

            <div className="settings-icon yellow">
              <Server size={18} />
            </div>

            <div>
              <h3>System status</h3>

              <p>
                Current application services.
              </p>
            </div>

          </div>

          <div className="settings-list">

            <div className="settings-row">

              <div className="settings-service">
                <Activity size={16} />
                <span>Backend API</span>
              </div>

              <span className="service-status healthy">
                ● Operational
              </span>

            </div>

            <div className="settings-row">

              <div className="settings-service">
                <ShieldCheck size={16} />
                <span>OSV integration</span>
              </div>

              <span className="service-status healthy">
                ● Available
              </span>

            </div>

          </div>

        </div>

      </section>

      {/* -------------------------------------------- */}
      {/* ABOUT */}
      {/* -------------------------------------------- */}

      <section className="panel about-panel">

        <div className="about-mark">
          ◈
        </div>

        <div>

          <div className="eyebrow">
            ABOUT
          </div>

          <h3>
            OpenChain Sentinel
          </h3>

          <p>
            Software supply-chain risk and
            dependency intelligence for
            developer projects.
          </p>

          <div className="about-stack">

            <span>React</span>
            <span>Spring Boot</span>
            <span>MongoDB</span>
            <span>Neo4j</span>
            <span>OSV</span>

          </div>

        </div>

      </section>

    </main>
  );
}

export default Settings;