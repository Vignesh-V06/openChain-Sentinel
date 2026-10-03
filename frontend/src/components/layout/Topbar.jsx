import { Bell, Search, Command } from "lucide-react";

function Topbar() {
  return (
    <header className="topbar">
      <div className="topbar-search">
        <Search size={17} />

        <input
          type="text"
          placeholder="Search vulnerabilities, packages..."
        />

        <div className="search-shortcut">
          <Command size={12} />
          <span>K</span>
        </div>
      </div>

      <div className="topbar-actions">
        <button className="icon-button">
          <Bell size={18} />
        </button>

        <div className="user-profile">
          <div className="avatar">VV</div>

          <div className="user-details">
            <span>Vignesh V</span>
            <small>Developer</small>
          </div>
        </div>
      </div>
    </header>
  );
}

export default Topbar;