
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Activity,
  ArrowUpRight,
  CheckCircle2,
  CircleUserRound,
  Clock3,
  Code2,
  Database,
  Fingerprint,
  Info,
  KeyRound,
  LogOut,
  Mail,
  Monitor,
  RefreshCw,
  Server,
  ShieldCheck,
  UserRound,
  XCircle,
} from "lucide-react";

import { useAuth } from "../context/AuthContext";
import { getHealth } from "../services/api";
import "./SettingsPage.css";

function getDisplayName(user) {
  return (
    user?.displayName ||
    user?.fullName ||
    user?.name ||
    user?.username ||
    "Sentinel User"
  );
}

function getInitials(user) {
  const name = getDisplayName(user).trim();
  const parts = name.split(/\s+/).filter(Boolean);

  if (!parts.length) return "U";

  return parts.length > 1
    ? `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase()
    : name.slice(0, 2).toUpperCase();
}

function formatDate(value) {
  if (!value) return "Not available";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) return "Not available";

  return new Intl.DateTimeFormat(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  }).format(date);
}

function DetailRow({ icon: Icon, label, value, secondary }) {
  return (
    <div className="settings-detail-row">
      <span className="settings-detail-icon">
        <Icon size={17} />
      </span>

      <div className="settings-detail-copy">
        <span className="settings-detail-label">{label}</span>
        <strong>{value || "Not available"}</strong>
        {secondary && (
          <span className="settings-detail-secondary">{secondary}</span>
        )}
      </div>
    </div>
  );
}

function StatusPill({ status }) {
  const healthy = status === "online";

  return (
    <span
      className={`settings-status-pill ${
        healthy ? "is-online" : "is-offline"
      }`}
    >
      {healthy ? <CheckCircle2 size={13} /> : <XCircle size={13} />}
      {healthy ? "Reachable" : "Unavailable"}
    </span>
  );
}

function Settings() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [apiStatus, setApiStatus] = useState("checking");
  const [apiMessage, setApiMessage] = useState("Checking backend connection...");
  const [lastChecked, setLastChecked] = useState(null);
  const [checking, setChecking] = useState(false);

  const displayName = getDisplayName(user);
  const initials = getInitials(user);
  const username = user?.username || "Not available";
  const email = user?.email || "Not available";
  const role = user?.role || "User";
  const provider = user?.provider || "LOCAL";
  const accountCreated = formatDate(user?.createdAt);

  async function checkBackend() {
    setChecking(true);

    try {
      const response = await getHealth();
      const data = response?.data ?? response;

      // A successful health endpoint means the backend HTTP service
      // responded. It does not prove every database is healthy.
      const status = String(
        data?.status ?? data?.health ?? "UP"
      ).toUpperCase();

      if (
        status === "DOWN" ||
        status === "ERROR" ||
        status === "OUT_OF_SERVICE"
      ) {
        setApiStatus("offline");
        setApiMessage("The backend reported an unhealthy status.");
      } else {
        setApiStatus("online");
        setApiMessage("The backend health endpoint responded successfully.");
      }

      setLastChecked(new Date());
    } catch {
      setApiStatus("offline");
      setApiMessage(
        "The backend health endpoint could not be reached. Check whether Spring Boot is running."
      );
      setLastChecked(new Date());
    } finally {
      setChecking(false);
    }
  }

  useEffect(() => {
    checkBackend();
  }, []);

  useEffect(() => {
    if (window.location.hash === "#profile") {
      requestAnimationFrame(() => {
        document.getElementById("profile")?.scrollIntoView({
          behavior: "smooth",
          block: "start",
        });
      });
    }
  }, []);

  function handleLogout() {
    if (!window.confirm("Are you sure you want to log out?")) {
      return;
    }

    logout();
    navigate("/login", { replace: true });
  }

  return (
    <div className="page-shell settings-page">
      <div className="page-header settings-page-header">
        <div>
          <span className="eyebrow">PREFERENCES & ACCOUNT</span>
          <h1>Settings</h1>
          <p>
            Manage your account information, review your session, and check
            the status of your OpenChain Sentinel application.
          </p>
        </div>

        <button
          type="button"
          className="settings-refresh-button"
          onClick={checkBackend}
          disabled={checking}
        >
          <RefreshCw
            size={15}
            className={checking ? "settings-spin" : ""}
          />
          {checking ? "Checking..." : "Refresh status"}
        </button>
      </div>

      <section className="settings-profile-banner" id="profile">
        <div className="settings-profile-main">
          <div className="settings-profile-avatar">{initials}</div>

          <div className="settings-profile-identity">
            <span className="settings-section-kicker">MY ACCOUNT</span>
            <h2>{displayName}</h2>
            <p>{email}</p>

            <div className="settings-profile-tags">
              <span className="settings-profile-tag">
                <ShieldCheck size={13} />
                Authenticated
              </span>
              <span className="settings-profile-tag">
                <UserRound size={13} />
                {role}
              </span>
            </div>
          </div>
        </div>

        <button
          type="button"
          className="settings-logout-button"
          onClick={handleLogout}
        >
          <LogOut size={16} />
          Log out
        </button>
      </section>

      <div className="settings-content-grid">
        <section className="settings-card settings-account-card">
          <div className="settings-card-heading">
            <div className="settings-card-heading-icon blue">
              <CircleUserRound size={19} />
            </div>

            <div>
              <h3>Profile information</h3>
              <p>Your account details returned by the backend</p>
            </div>
          </div>

          <div className="settings-detail-list">
            <DetailRow
              icon={UserRound}
              label="Display name"
              value={displayName}
            />

            <DetailRow
              icon={Fingerprint}
              label="Username"
              value={username}
            />

            <DetailRow
              icon={Mail}
              label="Email address"
              value={email}
            />

            <DetailRow
              icon={ShieldCheck}
              label="Account role"
              value={role}
              secondary="Role provided by your authenticated account"
            />

            <DetailRow
              icon={KeyRound}
              label="Sign-in provider"
              value={provider}
            />

            <DetailRow
              icon={Clock3}
              label="Account created"
              value={accountCreated}
            />
          </div>

          <div className="settings-information-note">
            <Info size={16} />
            <p>
              Profile details are currently read-only. Editing your name,
              email or password requires corresponding backend update
              endpoints, which are not part of the current authentication API.
            </p>
          </div>
        </section>

        <div className="settings-side-column">
          <section className="settings-card">
            <div className="settings-card-heading">
              <div className="settings-card-heading-icon green">
                <ShieldCheck size={19} />
              </div>

              <div>
                <h3>Account security</h3>
                <p>Current session information</p>
              </div>
            </div>

            <div className="settings-security-state">
              <span className="settings-security-check">
                <CheckCircle2 size={19} />
              </span>

              <div>
                <strong>Signed in</strong>
                <p>
                  Your current session uses token-based authentication.
                </p>
              </div>
            </div>

            <div className="settings-security-footnote">
              Signing out clears your local authentication token. It does not
              delete your account or project data.
            </div>

            <button
              type="button"
              className="settings-secondary-action"
              onClick={handleLogout}
            >
              <LogOut size={15} />
              Sign out of this session
            </button>
          </section>

          <section className="settings-card">
            <div className="settings-card-heading">
              <div className="settings-card-heading-icon blue">
                <Activity size={19} />
              </div>

              <div>
                <h3>Backend connection</h3>
                <p>Spring Boot API reachability</p>
              </div>
            </div>

            <div className="settings-service-row">
              <span className="settings-service-icon">
                <Server size={17} />
              </span>

              <div className="settings-service-copy">
                <strong>OpenChain API</strong>
                <span>localhost:8080</span>
              </div>

              {apiStatus !== "checking" && (
                <StatusPill status={apiStatus} />
              )}
            </div>

            {apiStatus === "checking" && (
              <div className="settings-service-message">
                {apiMessage}
              </div>
            )}

            {apiStatus !== "checking" && (
              <div
                className={`settings-service-message ${
                  apiStatus === "online" ? "success" : "error"
                }`}
              >
                {apiMessage}
              </div>
            )}

            {lastChecked && (
              <div className="settings-last-checked">
                Last checked at{" "}
                {lastChecked.toLocaleTimeString([], {
                  hour: "2-digit",
                  minute: "2-digit",
                  second: "2-digit",
                })}
              </div>
            )}
          </section>

          <section className="settings-card settings-app-card">
            <div className="settings-card-heading">
              <div className="settings-card-heading-icon purple">
                <Code2 size={19} />
              </div>

              <div>
                <h3>Application details</h3>
                <p>About this workspace</p>
              </div>
            </div>

            <div className="settings-app-row">
              <span>Application</span>
              <strong>OpenChain Sentinel</strong>
            </div>

            <div className="settings-app-row">
              <span>Frontend</span>
              <strong>React + Vite</strong>
            </div>

            <div className="settings-app-row">
              <span>Backend</span>
              <strong>Spring Boot</strong>
            </div>

            <div className="settings-app-row">
              <span>Metadata store</span>
              <strong>
                <Database size={13} />
                MongoDB
              </strong>
            </div>

            <div className="settings-app-row">
              <span>Dependency graph</span>
              <strong>
                <Monitor size={13} />
                Neo4j
              </strong>
            </div>

            <div className="settings-app-footer">
              <span>
                <ShieldCheck size={14} />
                Supply-chain security workspace
              </span>

              <button
                type="button"
                onClick={() => navigate("/dashboard")}
                aria-label="Open dashboard"
              >
                Dashboard
                <ArrowUpRight size={14} />
              </button>
            </div>
          </section>
        </div>
      </div>

      <div className="settings-page-footer">
        <span>OpenChain Sentinel</span>
        <span>See the chain. Understand the risk. Secure the software.</span>
      </div>
    </div>
  );
}

export default Settings;