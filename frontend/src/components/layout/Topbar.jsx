import { Bell } from "lucide-react";

function Topbar() {
  return (
    <header className="topbar">
      <div className="topbar-quote">
        <span className="quote-mark">“</span>
        <span>
          See the chain. Understand the risk. Secure the software.
        </span>
        <span className="quote-mark">”</span>
      </div>

      <div className="topbar-actions">
        <button
          type="button"
          className="icon-button"
          aria-label="Notifications"
        >
          <Bell size={18} strokeWidth={1.7} />
        </button>

        <div className="user-profile">
          <div className="avatar">
            V
          </div>

          <div className="user-details">
            <span>Batsie</span>
            <small>I'm Vengence</small>
          </div>
        </div>
      </div>
    </header>
  );
}

export default Topbar;