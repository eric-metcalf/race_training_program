import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { differenceInCalendarDays, format, parseISO } from "date-fns";
import { clsx } from "clsx";
import { api, type PlanSummaryView } from "../api/client";

export const Route = createFileRoute("/plans")({
  component: Plans,
});

function Plans() {
  const q = useQuery({ queryKey: ["plans"], queryFn: () => api.plans() });

  if (q.isLoading) return <p className="text-stone-500">Loading…</p>;
  if (q.error) return <p className="text-red-700">Error: {String(q.error)}</p>;
  if (!q.data) return null;

  // Sort: active first, then upcoming races (date asc), then past (date desc).
  const today = new Date();
  const sorted = [...q.data].sort((a, b) => {
    if (a.isActive && !b.isActive) return -1;
    if (b.isActive && !a.isActive) return 1;
    const aFut = parseISO(a.raceDate) >= today;
    const bFut = parseISO(b.raceDate) >= today;
    if (aFut && !bFut) return -1;
    if (bFut && !aFut) return 1;
    if (aFut && bFut) return a.raceDate.localeCompare(b.raceDate);
    return b.raceDate.localeCompare(a.raceDate);
  });

  return (
    <div className="space-y-5">
      <div className="flex items-baseline justify-between">
        <h2 className="text-2xl font-bold">Your training plans</h2>
        <Link
          to="/templates"
          className="text-sm bg-stone-800 text-white px-3 py-1.5 rounded hover:bg-stone-900"
        >
          + New plan from template
        </Link>
      </div>

      {sorted.length === 0 ? (
        <div className="bg-white border border-stone-200 rounded-lg p-8 text-center">
          <p className="text-stone-700 mb-3">You don't have any plans yet.</p>
          <Link
            to="/templates"
            className="inline-block bg-stone-800 text-white text-sm px-4 py-2 rounded hover:bg-stone-900"
          >
            Pick a template
          </Link>
        </div>
      ) : (
        <div className="space-y-3">
          {sorted.map((p) => <PlanCard key={p.planId} p={p} />)}
        </div>
      )}
    </div>
  );
}

function PlanCard({ p }: { p: PlanSummaryView }) {
  const qc = useQueryClient();
  const navigate = useNavigate();
  const raceDate = parseISO(p.raceDate);
  const daysOut = differenceInCalendarDays(raceDate, new Date());
  const past = daysOut < 0;

  const activate = useMutation({
    mutationFn: () => api.activatePlan(p.planId),
    onSuccess: async () => {
      await qc.invalidateQueries();
      navigate({ to: "/plan" });
    },
  });

  const del = useMutation({
    mutationFn: () => api.deletePlan(p.planId),
    onSuccess: () => qc.invalidateQueries(),
  });

  return (
    <section
      className={clsx(
        "bg-white border rounded-lg p-5 transition-colors",
        p.isActive ? "border-emerald-400 ring-1 ring-emerald-200" : "border-stone-200",
      )}
    >
      <div className="flex items-baseline justify-between gap-3 mb-1">
        <h3 className="font-semibold text-stone-900">{p.raceName}</h3>
        {p.isActive && (
          <span className="text-xs uppercase tracking-wide bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded">
            Active
          </span>
        )}
      </div>
      <p className="text-sm text-stone-600">
        {format(raceDate, "EEEE, MMM d, yyyy")}
        {p.location && ` · ${p.location}`}
        {" · "}
        <span className={past ? "text-stone-500" : "font-medium"}>
          {past ? `${-daysOut} days ago` : `${daysOut} days to go`}
        </span>
      </p>
      <p className="text-xs text-stone-500 mt-1">
        {p.templateName ?? `(${p.templateKey})`} · {p.workoutCount} workouts ·{" "}
        {(p.distanceM / 1609.34).toFixed(1)} mi
        {p.vertM > 0 && ` · ${Math.round(p.vertM * 3.281).toLocaleString()} ft vert`}
      </p>
      <div className="flex items-center gap-3 mt-3">
        {!p.isActive && (
          <button
            onClick={() => activate.mutate()}
            disabled={activate.isPending}
            className="text-sm bg-stone-800 text-white px-3 py-1.5 rounded hover:bg-stone-900 disabled:opacity-50"
          >
            {activate.isPending ? "Activating…" : "Make active"}
          </button>
        )}
        {p.isActive && (
          <Link
            to="/plan"
            className="text-sm bg-stone-100 text-stone-800 px-3 py-1.5 rounded hover:bg-stone-200"
          >
            Open calendar
          </Link>
        )}
        <button
          onClick={() => {
            if (confirm(`Delete "${p.raceName}" and all its workouts? This cannot be undone.`)) {
              del.mutate();
            }
          }}
          disabled={del.isPending}
          className="text-sm text-stone-500 hover:text-red-700 ml-auto"
        >
          {del.isPending ? "Deleting…" : "Delete"}
        </button>
      </div>
      {(activate.error || del.error) && (
        <p className="text-sm text-red-700 mt-2">
          {String(activate.error ?? del.error)}
        </p>
      )}
    </section>
  );
}
