import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { differenceInCalendarDays, format, parseISO } from "date-fns";
import { api } from "../api/client";
import { ThisWeekPanel } from "../components/ThisWeekPanel";

export const Route = createFileRoute("/")({
  component: Dashboard,
});

function Dashboard() {
  const race = useQuery({
    queryKey: ["race"],
    queryFn: () => api.race(),
    retry: false,
  });

  if (race.isLoading) return <p className="text-stone-500">Loading…</p>;

  // 404 = no active plan yet.
  if (race.error)
    return (
      <div className="bg-white border border-stone-200 rounded-lg p-8 text-center space-y-4">
        <h2 className="text-xl font-bold text-stone-800">No active training plan</h2>
        <p className="text-stone-600">
          Pick a template to get started — a Leadville mountain marathon plan, classic Hal Higdon,
          or the Hanson Marathon Method.
        </p>
        <Link
          to="/templates"
          className="inline-block bg-stone-800 text-white text-sm px-4 py-2 rounded hover:bg-stone-900"
        >
          Browse templates
        </Link>
      </div>
    );
  if (!race.data) return null;

  const r = race.data;
  const date = parseISO(r.raceDate);
  const daysOut = differenceInCalendarDays(date, new Date());

  return (
    <div className="space-y-6">
      <section className="bg-white border border-stone-200 rounded-lg p-6">
        <p className="text-sm uppercase tracking-wide text-stone-500">Next race</p>
        <h2 className="text-2xl font-bold mt-1">{r.name}</h2>
        <p className="text-stone-600 mt-1">
          {format(date, "EEEE, MMMM d, yyyy")} · {r.location ?? "—"}
        </p>
        <p className="text-stone-600">
          {(r.distanceM / 1609.34).toFixed(1)} mi
          {r.vertM > 0 && ` · ${Math.round(r.vertM * 3.281).toLocaleString()} ft of gain`}
        </p>
        <p className="text-5xl font-bold mt-4 text-stone-900">
          {daysOut}{" "}
          <span className="text-base font-normal text-stone-500">days to go</span>
        </p>
        {r.notes && <p className="text-stone-500 text-sm mt-3 italic">{r.notes}</p>}
      </section>

      <ThisWeekPanel />

      <p className="text-xs text-stone-400 text-center">
        Want a different plan?{" "}
        <Link to="/plans" className="underline hover:text-stone-700">
          Switch active plan
        </Link>{" "}
        or{" "}
        <Link to="/templates" className="underline hover:text-stone-700">
          start a new one from a template
        </Link>
        .
      </p>
    </div>
  );
}
