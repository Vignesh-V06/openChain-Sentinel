import Sidebar from "./Sidebar";
import Topbar from "./Topbar";

function AppShell({ children }) {
  return (
    <div className="app-shell">
      <Sidebar />

      <div className="app-main">
        <Topbar />

        <main className="app-content">
          {children}
        </main>
      </div>
    </div>
  );
}

export default AppShell;