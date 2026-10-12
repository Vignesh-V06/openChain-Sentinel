
import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Bell,
  ChevronDown,
  CircleUserRound,
  LogOut,
  Settings,
  ShieldCheck,
} from "lucide-react";

import { useAuth } from "../../context/AuthContext";
import "./Topbar.css";

function getDisplayName(user) {
  return (
    user?.displayName ||
    user?.fullName ||
    user?.name ||
    user?.username ||
    user?.email?.split("@")[0] ||
    "Sentinel User"
  );
}

function getInitials(user) {
  const name = getDisplayName(user).trim();

  if (!name) return "U";

  const parts = name.split(/\s+/);

  return parts.length > 1
    ? `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase()
    : name.slice(0, 2).toUpperCase();
}

function Topbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [menuOpen, setMenuOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);

  const menuRef = useRef(null);
  const notificationRef = useRef(null);

  const displayName = getDisplayName(user);
  const username = user?.username || "";
  const email = user?.email || "";
  const initials = getInitials(user);

  useEffect(() => {
    function handleOutsideClick(event) {
      if (
        menuRef.current &&
        !menuRef.current.contains(event.target)
      ) {
        setMenuOpen(false);
      }

      if (
        notificationRef.current &&
        !notificationRef.current.contains(event.target)
      ) {
        setNotificationsOpen(false);
      }
    }

    function handleEscape(event) {
      if (event.key === "Escape") {
        setMenuOpen(false);
        setNotificationsOpen(false);
      }
    }

    document.addEventListener("mousedown", handleOutsideClick);
    document.addEventListener("keydown", handleEscape);

    return () => {
      document.removeEventListener("mousedown", handleOutsideClick);
      document.removeEventListener("keydown", handleEscape);
    };
  }, []);

  function openSettings() {
    setMenuOpen(false);
    navigate("/settings");
  }

  function openProfile() {
    setMenuOpen(false);
    navigate("/settings#profile");
  }

  function handleLogout() {
    setMenuOpen(false);

    if (!window.confirm("Are you sure you want to log out?")) {
      return;
    }

    logout();
    navigate("/login", { replace: true });
  }

  return (
    <header className="topbar">
      <div className="topbar-quote">
        <span className="topbar-quote-mark">“</span>
        See the chain. Understand the risk. Secure the software.
        <span className="topbar-quote-mark">”</span>
      </div>

      <div className="topbar-actions">
        <div className="topbar-notification-wrap" ref={notificationRef}>
          <button
            type="button"
            className={`topbar-icon-button ${
              notificationsOpen ? "is-active" : ""
            }`}
            aria-label="Notifications"
            aria-expanded={notificationsOpen}
            onClick={() => {
              setNotificationsOpen((open) => !open);
              setMenuOpen(false);
            }}
          >
            <Bell size={19} strokeWidth={1.7} />
          </button>

          {notificationsOpen && (
            <div className="topbar-popover topbar-notifications">
              <div className="topbar-popover-heading">
                <strong>Notifications</strong>
                <span className="topbar-muted-label">Activity</span>
              </div>

              <div className="topbar-empty-notifications">
                <span className="topbar-empty-icon">
                  <Bell size={20} />
                </span>
                <strong>You're all caught up</strong>
                <p>
                  Scan alerts and security notifications will appear here
                  when they are available.
                </p>
              </div>
            </div>
          )}
        </div>

        <div className="topbar-account-wrap" ref={menuRef}>
          <button
            type="button"
            className="topbar-account-trigger"
            aria-label="Open account menu"
            aria-expanded={menuOpen}
            onClick={() => {
              setMenuOpen((open) => !open);
              setNotificationsOpen(false);
            }}
          >
            <span className="topbar-avatar">{initials}</span>

            <span className="topbar-account-copy">
              <strong>{displayName}</strong>
              <span>{username ? `@${username}` : email || "My account"}</span>
            </span>

            <ChevronDown
              className={`topbar-account-chevron ${
                menuOpen ? "is-open" : ""
              }`}
              size={15}
            />
          </button>

          {menuOpen && (
            <div className="topbar-popover topbar-account-menu">
              <div className="topbar-menu-user">
                <span className="topbar-avatar topbar-avatar-large">
                  {initials}
                </span>

                <div className="topbar-menu-user-copy">
                  <strong>{displayName}</strong>
                  <span>{email || (username ? `@${username}` : "Account")}</span>
                </div>
              </div>

              <div className="topbar-menu-divider" />

              <button
                type="button"
                className="topbar-menu-item"
                onClick={openProfile}
              >
                <CircleUserRound size={17} />
                <span>My profile</span>
              </button>

              <button
                type="button"
                className="topbar-menu-item"
                onClick={openSettings}
              >
                <Settings size={17} />
                <span>Settings</span>
              </button>

              <div className="topbar-menu-divider" />

              <div className="topbar-menu-security">
                <ShieldCheck size={15} />
                <span>Authenticated session</span>
              </div>

              <button
                type="button"
                className="topbar-menu-item topbar-menu-logout"
                onClick={handleLogout}
              >
                <LogOut size={17} />
                <span>Log out</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

export default Topbar;
