import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { format, parseISO, startOfWeek, addDays, isToday, subWeeks, addWeeks } from "date-fns";
import { clsx } from "clsx";
import { api, type PlannedWorkoutView } from "../api/client";
import { TypeBadge, StatusBadge } from "../components/WorkoutBadge";
import { WorkoutPanel } from "../components/WorkoutPanel";
import { mi, ft, minSec } from "../lib/format";

export const Route = createFileRoute("/plan")({
  component: PlanCalendar,
});

function PlanCalendar() {
  const qc = useQueryClient();
  const [expandedDate, setExpandedDate] = useState<string | null>(null);

  const race = useQuery({ queryKey: ["race"], queryFn: () => api.race(), retry: false });

  // Derive a generous window around the race so we cover any 9-22 week template.
  // Trim empty weeks from the rendered list.
  const raceDate = race.data ? parseISO(race.data.raceDate) : null;
  const planRangeFrom = raceDate ? format(subWeeks(raceDate, 22), "yyyy-MM-dd") : null;
  const planRangeTo   = raceDate ? format(addWeeks(raceDate, 1),  "yyyy-MM-dd") : null;

  const plan = useQuery({
    enabled: !!planRangeFrom && !!planRangeTo,
    queryKey: ["plan", planRangeFrom, planRangeTo],
    queryFn: () => api.plan(planRangeFrom!, planRangeTo!),
  });

  const regen = useMutation({
    mutationFn: () => api.regenerate(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["plan"] });
      setExpandedDate(null);
    },
  });

  if (race.isLoading) return <p className="text-stone-500">Loading…</p>;
  if (race.error)
    return (
      <div className="bg-white border border-stone-200 rounded-lg p-6 text-center">
        <p className="text-stone-700 mb-3">No active plan.</p>
        <Link
          to="/templates"
          className="inline-block bg-stone-800 text-white text-sm px-3 py-2 rounded"
        >
          Browse templates
        </Link>
      </div>
    );
  if (plan.isLoading || !race.data) return <p className="text-stone-500">Loading…</p>;
  if (plan.error) return <p className="text-red-700">Error: {String(plan.error)}</p>;
  if (!plan.data) return null;

  const byDate = new Map(plan.data.map((w) => [w.date, w]));
  const weeks = groupByWeek(plan.data);
  const toggle = (d: string) => setExpandedDate((cur) => (cur === d ? null : d));

  return (
    <div className="space-y-6">
      <div className="flex items-baseline justify-between">
        <h2 className="text-xl font-bold">Training plan — {race.data.name}</h2>
        <Link to="/templates" className="text-sm text-stone-500 hover:text-stone-800">
          Switch template
        </Link>
      </div>

      <div className="space-y-3">
        {weeks.map((week) => {
          const weekStart = parseISO(week.workouts[0].date);
          const weekOf = startOfWeek(weekStart, { weekStartsOn: 1 });
          const weekDates = Array.from({ length: 7 }, (_, i) =>
            format(addDays(weekOf, i), "yyyy-MM-dd"),
          );
          const expandedHere = expandedDate && weekDates.includes(expandedDate)
            ? expandedDate
            : null;

          // Per-week totals across all workouts in this week (rest/xtrain rows
          // don't contribute since they have null targets).
          const totalMiles = week.workouts.reduce(
            (s, w) => s + (w.targetDistanceM ?? 0),
            0,
          ) / 1609.34;
          const totalVertFt = week.workouts.reduce(
            (s, w) => s + (w.targetVertM ?? 0),
            0,
          ) * 3.281;
          const runDays = week.workouts.filter(
            (w) => w.type !== "rest" && w.type !== "xtrain",
          ).length;

          return (
            <section
              key={week.id}
              className="bg-white border border-stone-200 rounded-lg overflow-hidden"
            >
              <header className="px-4 py-2 border-b border-stone-100 bg-stone-50 flex items-center justify-between">
                <span className="text-xs uppercase tracking-wide text-stone-500">
                  Week of {format(weekOf, "MMM d")}
                </span>
                <span className="text-xs text-stone-500 space-x-3 font-medium">
                  {totalMiles > 0 && <span>{totalMiles.toFixed(1)} mi</span>}
                  {totalVertFt > 0 && (
                    <span>↑ {Math.round(totalVertFt).toLocaleString()} ft</span>
                  )}
                  <span className="text-stone-400">
                    {runDays} run{runDays === 1 ? "" : "s"}
                  </span>
                </span>
              </header>
              <div className="grid grid-cols-7 divide-x divide-stone-100">
                {weekDates.map((d) => {
                  const w = byDate.get(d);
                  return (
                    <DayCell
                      key={d}
                      date={parseISO(d)}
                      workout={w}
                      expanded={expandedDate === d}
                      onClick={() => toggle(d)}
                    />
                  );
                })}
              </div>
              {expandedHere && (
                <WorkoutPanel
                  date={expandedHere}
                  onClose={() => setExpandedDate(null)}
                />
              )}
            </section>
          );
        })}
      </div>

      <div className="flex justify-center pt-4 pb-2">
        <button
          onClick={() => {
            if (confirm("Regenerate plan from template? This overwrites any edits.")) {
              regen.mutate();
            }
          }}
          disabled={regen.isPending}
          className="text-sm text-stone-500 hover:text-stone-800 underline"
        >
          {regen.isPending ? "Regenerating…" : "Regenerate plan"}
        </button>
      </div>
    </div>
  );
}

function DayCell({
  date,
  workout,
  expanded,
  onClick,
}: {
  date: Date;
  workout: PlannedWorkoutView | undefined;
  expanded: boolean;
  onClick: () => void;
}) {
  const today = isToday(date);
  return (
    <button
      type="button"
      onClick={onClick}
      className={clsx(
        "block text-left min-h-[8rem] p-2 hover:bg-stone-50 transition-colors w-full",
        today && "bg-yellow-50",
        expanded && "bg-stone-100 ring-1 ring-inset ring-stone-300",
      )}
    >
      <div className="flex items-center justify-between text-xs text-stone-500 mb-1">
        <span>{format(date, "EEE d")}</span>
        {today && <span className="font-bold text-yellow-700">today</span>}
      </div>
      {workout ? <WorkoutSummary w={workout} /> : <span className="text-xs text-stone-300">—</span>}
    </button>
  );
}

function WorkoutSummary({ w }: { w: PlannedWorkoutView }) {
  const distance = mi(w.targetDistanceM);
  const duration = minSec(w.targetDurationS);
  const vert = ft(w.targetVertM);
  return (
    <div className="space-y-1">
      <TypeBadge type={w.type} />
      <div className="text-sm font-medium text-stone-800">
        {distance ?? duration ?? w.intensity ?? "Workout"}
      </div>
      <div className="text-xs text-stone-500 space-y-0.5">
        {distance && duration && <div>{duration}</div>}
        {vert && <div>↑ {vert}</div>}
      </div>
      <StatusBadge status={w.matchStatus} />
    </div>
  );
}

interface WeekGroup {
  id: string;
  workouts: PlannedWorkoutView[];
}

function groupByWeek(workouts: PlannedWorkoutView[]): WeekGroup[] {
  const sorted = [...workouts].sort((a, b) => a.date.localeCompare(b.date));
  const groups: WeekGroup[] = [];
  for (const w of sorted) {
    const wk = startOfWeek(parseISO(w.date), { weekStartsOn: 1 });
    const key = format(wk, "yyyy-MM-dd");
    let g = groups.find((x) => x.id === key);
    if (!g) {
      g = { id: key, workouts: [] };
      groups.push(g);
    }
    g.workouts.push(w);
  }
  return groups.filter((g) => g.workouts.length > 0);
}
