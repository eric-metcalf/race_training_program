import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/settings")({
  component: Settings,
});

function Settings() {
  return (
    <div className="space-y-4">
      <h2 className="text-xl font-bold">Settings</h2>
      <a
        href="/api/strava/connect"
        className="inline-block bg-orange-600 hover:bg-orange-700 text-white font-medium px-4 py-2 rounded-md"
      >
        Connect to Strava
      </a>
    </div>
  );
}
