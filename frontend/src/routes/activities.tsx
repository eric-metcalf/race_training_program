import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { format, parseISO, isToday, isYesterday } from "date-fns";
import { clsx } from "clsx";
import { api, type ActivityView } from "../api/client";
import { mi, ft, minSec } from "../lib/format";

export const Route = createFileRoute("/activities")({
  component: Activities,
});

function Activities() {
  const q = useQuery({ queryKey: ["activities"], queryFn: () => api.activities(200) });

  if (q.isLoading) return <p className="text-stone-500">Loading…</p>;
  if (q.error) return <p className="text-red-700">Error: {String(q.error)}</p>;
  if (!q.data) return null;

  const totalDist = q.data.reduce((s, a) => s + a.distanceM, 0);
  const totalTime = q.data.reduce((s, a) => s + a.movingSeconds, 0);

  return (
    <div className="space-y-5">
      <div className="flex items-baseline justify-between">
        <h2 className="text-2xl font-bold">Activities</h2>
        <Link
          to="/upload"
          className="text-sm bg-stone-800 text-white px-3 py-1.5 rounded hover:bg-stone-900"
        >
          + Upload .FIT
        </Link>
      </div>

      {q.data.length === 0 ? (
        <div className="bg-white border border-stone-200 rounded-lg p-8 text-center">
          <p className="text-stone-700 mb-3">No activities yet.</p>
          <p className="text-stone-500 text-sm mb-4">
            Drop a .FIT file from Garmin Connect or your watch to import a session.
          </p>
          <Link
            to="/upload"
            className="inline-block bg-stone-800 text-white text-sm px-4 py-2 rounded hover:bg-stone-900"
          >
            Upload .FIT
          </Link>
        </div>
      ) : (
        <>
          <div className="bg-white border border-stone-200 rounded-lg p-4 grid grid-cols-3 gap-4">
            <Stat label="Activities" value={String(q.data.length)} />
            <Stat label="Total distance" value={mi(totalDist) ?? "—"} />
            <Stat label="Total moving time" value={minSec(totalTime) ?? "—"} />
          </div>

          <ul className="bg-white border border-stone-200 rounded-lg divide-y divide-stone-100">
            {groupByDay(q.data).map(([day, items]) => (
              <li key={day}>
                <DayHeader day={day} />
                <ul className="divide-y divide-stone-50">
                  {items.map((a) => <ActivityRow key={a.id} a={a} />)}
                </ul>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}

function ActivityRow({ a }: { a: ActivityView }) {
  const dt = parseISO(a.startedAt);
  const dateKey = format(dt, "yyyy-MM-dd");
  return (
    <li className="px-4 py-3 hover:bg-stone-50 transition-colors">
      <div className="flex items-baseline justify-between gap-3">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2 text-xs text-stone-500 mb-0.5">
            <span>{format(dt, "h:mm a")}</span>
            <SourceBadge source={a.source} />
            <ActivityTypeChip type={a.activityType} />
          </div>
          <p className="text-stone-900 truncate">{a.name ?? "(unnamed)"}</p>
          <Link
            to="/plan/$date"
            params={{ date: dateKey }}
            className="text-xs text-stone-400 hover:text-stone-700 hover:underline"
          >
            See planned workout for {format(dt, "MMM d")} →
          </Link>
        </div>
        <dl className="grid grid-cols-3 gap-x-4 text-sm text-right shrink-0">
          <Stat compact label="Dist" value={mi(a.distanceM)} />
          <Stat compact label="Time" value={minSec(a.movingSeconds)} />
          <Stat compact label="Vert" value={ft(a.elevationGainM)} />
        </dl>
      </div>
    </li>
  );
}

function DayHeader({ day }: { day: string }) {
  const dt = parseISO(day);
  let label = format(dt, "EEEE, MMM d");
  if (isToday(dt)) label = `Today — ${format(dt, "MMM d")}`;
  else if (isYesterday(dt)) label = `Yesterday — ${format(dt, "MMM d")}`;
  return (
    <div className="px-4 py-2 bg-stone-50 text-xs uppercase tracking-wide text-stone-500 border-b border-stone-100">
      {label}
    </div>
  );
}

function Stat({
  label,
  value,
  compact = false,
}: {
  label: string;
  value: string | null;
  compact?: boolean;
}) {
  return (
    <div>
      <div className={clsx("text-stone-500", compact ? "text-[10px]" : "text-xs")}>{label}</div>
      <div className={clsx("font-medium text-stone-900", compact ? "text-sm" : "")}>
        {value ?? "—"}
      </div>
    </div>
  );
}

function SourceBadge({ source }: { source: string }) {
  const styles: Record<string, string> = {
    fit: "bg-blue-100 text-blue-800 ring-blue-200",
    strava: "bg-orange-100 text-orange-800 ring-orange-200",
    manual: "bg-stone-100 text-stone-700 ring-stone-200",
  };
  const label = source === "fit" ? "FIT" : source.charAt(0).toUpperCase() + source.slice(1);
  return (
    <span
      className={clsx(
        "inline-block px-1.5 py-0.5 rounded text-[10px] uppercase tracking-wide ring-1",
        styles[source] ?? styles.manual,
      )}
    >
      {label}
    </span>
  );
}

function ActivityTypeChip({ type }: { type: string }) {
  return (
    <span className="inline-block px-1.5 py-0.5 rounded text-[10px] uppercase tracking-wide bg-stone-100 text-stone-600 ring-1 ring-stone-200">
      {type.replace(/_/g, " ")}
    </span>
  );
}

function groupByDay(activities: ActivityView[]): [string, ActivityView[]][] {
  // Sorted by started_at desc from the API; group preserving order.
  const groups = new Map<string, ActivityView[]>();
  for (const a of activities) {
    const key = format(parseISO(a.startedAt), "yyyy-MM-dd");
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key)!.push(a);
  }
  return Array.from(groups.entries());
}
