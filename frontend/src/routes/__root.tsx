import { Outlet, Link, createRootRoute } from "@tanstack/react-router";
import { clsx } from "clsx";

export const Route = createRootRoute({
  component: RootLayout,
});

function RootLayout() {
  return (
    <div className="min-h-screen flex flex-col">
      <header className="border-b border-stone-200 bg-white">
        <div className="max-w-5xl mx-auto px-6 py-4 flex items-center gap-6">
          <h1 className="font-bold text-lg text-stone-800">Race Training</h1>
          <nav className="flex gap-4 text-sm">
            <NavLink to="/">Dashboard</NavLink>
            <NavLink to="/plan">Plan</NavLink>
            <NavLink to="/plans">My plans</NavLink>
            <NavLink to="/templates">Templates</NavLink>
            <NavLink to="/activities">Activities</NavLink>
            <NavLink to="/upload">Upload</NavLink>
          </nav>
        </div>
      </header>
      <main className="flex-1">
        <div className="max-w-5xl mx-auto px-6 py-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}

function NavLink({ to, children }: { to: string; children: React.ReactNode }) {
  return (
    <Link
      to={to}
      className={clsx(
        "text-stone-600 hover:text-stone-900 transition-colors",
        "[&.active]:text-stone-900 [&.active]:font-medium",
      )}
    >
      {children}
    </Link>
  );
}
