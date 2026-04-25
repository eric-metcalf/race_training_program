import { Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import {
  startOfWeek,
  endOfWeek,
  addDays,
  format,
  parseISO,
  isToday,
  isPast,
  isSameDay,
} from "date-fns";
import { clsx } from "clsx";
import { api, type PlannedWorkoutView } from "../api/client";
import { mi } from "../lib/format";

const DONE_STATUSES = new Set(["completed", "partial", "modified"]);

export function ThisWeekPanel() {
  const today = new Date();
  const weekStart = startOfWeek(today, { weekStartsOn: 1 });
  const weekEnd = endOfWeek(today, { weekStartsOn: 1 });
  const fromStr = format(weekStart, "yyyy-MM-dd");
  const toStr = format(weekEnd, "yyyy-MM-dd");

  const plan = useQuery({
    queryKey: ["plan", "week", fromStr, toStr],
    queryFn: () => api.plan(fromStr, toStr),
  });
  const acts = useQuery({
    queryKey: ["activities"],
    queryFn: () => api.activities(200),
  });

  if (plan.isLoading || acts.isLoading) {
    return <PanelShell><p className="text-stone-500">Loading…</p></PanelShell>;
  }
  if (plan.error) {
    return <PanelShell><p className="text-red-700">Error: {String(plan.error)}</p></PanelShell>;
  }
  if (!plan.data) return null;

  const planned = plan.data;
  const weekActivities = (acts.data ?? []).filter((a) => {
    const d = parseISO(a.startedAt);
    return d >= weekStart && d <= weekEnd;
  });

  const runDays = planned.filter((p) => p.type !== "rest" && p.type !== "xtrain");
  const doneDays = runDays.filter((p) => DONE_STATUSES.has(p.matchStatus));
  const plannedMiles =
    runDays.reduce((s, p) => s + (p.targetDistanceM ?? 0), 0) / 1609.34;
  const loggedMiles =
    weekActivities.reduce((s, a) => s + a.distanceM, 0) / 1609.34;

  const days = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i));

  return (
    <PanelShell>
      <div className="flex items-baseline justify-between mb-3">
        <div>
          <p className="text-sm uppercase tracking-wide text-stone-500">This week</p>
          <h3 className="font-semibold text-stone-900">
            {format(weekStart, "MMM d")} – {format(weekEnd, "MMM d")}
          </h3>
        </div>
        <Link to="/plan" className="text-sm text-stone-500 hover:text-stone-800">
          Open calendar →
        </Link>
      </div>

      <div className="grid grid-cols-2 gap-4 mb-4">
        <Stat
          label="Workouts done"
          value={`${doneDays.length} of ${runDays.length}`}
        />
        <Stat
          label="Mileage"
          value={`${loggedMiles.toFixed(1)} / ${plannedMiles.toFixed(1)} mi`}
          subtle={
            plannedMiles > 0
              ? `${Math.round((loggedMiles / plannedMiles) * 100)}% of plan`
              : null
          }
        />
      </div>

      <div className="grid grid-cols-7 gap-2">
        {days.map((d) => {
          const w = planned.find((p) => isSameDay(parseISO(p.date), d));
          return <DayPill key={d.toISOString()} date={d} w={w} />;
        })}
      </div>
    </PanelShell>
  );
}

function PanelShell({ children }: { children: React.ReactNode }) {
  return <section className="bg-white border border-stone-200 rounded-lg p-6">{children}</section>;
}

function Stat({
  label,
  value,
  subtle,
}: {
  label: string;
  value: string;
  subtle?: string | null;
}) {
  return (
    <div>
      <div className="text-xs text-stone-500 uppercase tracking-wide">{label}</div>
      <div className="text-xl font-bold text-stone-900">{value}</div>
      {subtle && <div className="text-xs text-stone-500 mt-0.5">{subtle}</div>}
    </div>
  );
}

function DayPill({ date, w }: { date: Date; w: PlannedWorkoutView | undefined }) {
  const dateStr = format(date, "yyyy-MM-dd");
  const todayFlag = isToday(date);
  const past = isPast(date) && !todayFlag;
  const status = w?.matchStatus ?? "pending";
  const done = DONE_STATUSES.has(status);
  const missed = status === "missed";

  const bg = done
    ? "bg-emerald-100 text-emerald-900 ring-emerald-200"
    : missed
      ? "bg-red-100 text-red-800 ring-red-200"
      : todayFlag
        ? "bg-yellow-100 text-yellow-900 ring-yellow-300"
        : past
          ? "bg-stone-100 text-stone-500 ring-stone-200"
          : "bg-stone-50 text-stone-700 ring-stone-200";

  const icon = done ? "✓" : missed ? "✕" : w?.type === "rest" ? "·" : todayFlag ? "•" : "";

  return (
    <Link
      to="/plan/$date"
      params={{ date: dateStr }}
      className={clsx("block text-center rounded ring-1 px-2 py-2 hover:opacity-80 transition-opacity", bg)}
    >
      <div className="text-[10px] uppercase tracking-wide opacity-70">
        {format(date, "EEE")}
      </div>
      <div className="text-sm font-semibold mt-0.5">{format(date, "d")}</div>
      {w && (
        <div className="text-[10px] mt-1 truncate" title={w.type}>
          {w.type === "rest" ? "rest" : mi(w.targetDistanceM)?.replace(" mi", "") ?? w.type}
        </div>
      )}
      {icon && <div className="text-xs mt-0.5">{icon}</div>}
    </Link>
  );
}
