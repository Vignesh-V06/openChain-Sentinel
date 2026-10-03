import {
  LayoutDashboard,
  FolderGit2,
  ShieldAlert,
  Network,
  Activity,
  History,
  Settings,
} from "lucide-react";

import {
  NavLink,
} from "react-router-dom";

function Sidebar() {

  const getNavClass =
    ({ isActive }) =>
      `nav-item ${
        isActive
          ? "active"
          : ""
      }`;

  return (
    <aside className="sidebar">

      {/* BRAND */}

      <div className="brand">

        <div className="brand-mark">
          ◇
        </div>

        <div>
          <div className="brand-name">
            OpenChain
          </div>

          <div className="brand-subtitle">
            SENTINEL
          </div>
        </div>

      </div>


      <div className="sidebar-navigation">

        {/* OVERVIEW */}

        <div className="nav-group">

          <div className="nav-section-title">
            Overview
          </div>


          <NavLink
            to="/dashboard"
            className={({ isActive }) => {
              const isScanDashboard =
                window.location.pathname.includes("/scans/");

              return `nav-item ${isActive || isScanDashboard
                  ? "active"
                  : ""
                }`;
            }}
          >
            <LayoutDashboard size={17} />

            <span>
              Dashboard
            </span>
          </NavLink>


          <NavLink
            to="/projects"
            end
            className={getNavClass}
          >
            <FolderGit2 size={17} />

            <span>
              Projects
            </span>
          </NavLink>

        </div>


        {/* SECURITY */}

        <div className="nav-group">

          <div className="nav-section-title">
            Security
          </div>


          <NavLink
            to="/vulnerabilities"
            className={getNavClass}
          >
            <ShieldAlert size={17} />

            <span>
              Vulnerabilities
            </span>
          </NavLink>


          <NavLink
            to="/dependency-graph"
            className={getNavClass}
          >
            <Network size={17} />

            <span>
              Dependency Graph
            </span>
          </NavLink>


          <NavLink
            to="/risk-analysis"
            className={getNavClass}
          >
            <Activity size={17} />

            <span>
              Risk Analysis
            </span>
          </NavLink>

        </div>


        {/* SCANS */}

        <div className="nav-group">

          <div className="nav-section-title">
            Scans
          </div>


          <NavLink
            to="/scan-history"
            className={getNavClass}
          >
            <History size={17} />

            <span>
              Scan History
            </span>
          </NavLink>

        </div>

      </div>


      {/* BOTTOM */}

      <div className="sidebar-bottom">

        <div className="system-status">

          <span className="status-dot" />

          <div>

            <div className="status-title">
              System Healthy
            </div>

            <div className="status-description">
              Backend connected
            </div>

          </div>

        </div>


        <NavLink
          to="/settings"
          className={getNavClass}
        >
          <Settings size={17} />

          <span>
            Settings
          </span>

        </NavLink>

      </div>

    </aside>
  );
}

export default Sidebar;