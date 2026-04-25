import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { format, parseISO } from "date-fns";
import { api } from "../api/client";
import { TypeBadge, StatusBadge } from "../components/WorkoutBadge";
import { mi, ft, minSec } from "../lib/format";

export const Route = createFileRoute("/plan/$date")({
  component: WorkoutDetail,
});

function WorkoutDetail() {
  const { date } = Route.useParams();
  const q = useQuery({
    queryKey: ["workout", date],
    queryFn: () => api.workout(date),
  });

  if (q.isLoading) return <p className="text-stone-500">Loading…</p>;
  if (q.error) return <p className="text-red-700">Error: {String(q.error)}</p>;
  if (!q.data) return null;

  const { planned, matchedActivity } = q.data;
  const dt = parseISO(planned.date);

  return (
    <div className="space-y-6">
      <div className="flex items-baseline justify-between">
        <div>
          <Link to="/plan" className="text-sm text-stone-500 hover:text-stone-800">
            ← Plan
          </Link>
          <h2 className="text-2xl font-bold mt-1">{format(dt, "EEEE, MMM d")}</h2>
        </div>
        <StatusBadge status={planned.matchStatus} />
      </div>

      <section className="bg-white border border-stone-200 rounded-lg p-5">
        <div className="flex items-center gap-2 mb-3">
          <TypeBadge type={planned.type} />
          {planned.intensity && (
            <span className="text-sm text-stone-600">{planned.intensity}</span>
          )}
        </div>
        <dl className="grid grid-cols-3 gap-4 text-sm">
          <div>
            <dt className="text-stone-500">Target distance</dt>
            <dd className="font-medium text-stone-900">{mi(planned.targetDistanceM) ?? "—"}</dd>
          </div>
          <div>
            <dt className="text-stone-500">Target duration</dt>
            <dd className="font-medium text-stone-900">{minSec(planned.targetDurationS) ?? "—"}</dd>
          </div>
          <div>
            <dt className="text-stone-500">Target vert</dt>
            <dd className="font-medium text-stone-900">{ft(planned.targetVertM) ?? "—"}</dd>
          </div>
        </dl>
        {planned.notes && (
          <p className="mt-4 text-sm italic text-stone-600 border-l-2 border-stone-200 pl-3">
            {planned.notes}
          </p>
        )}
      </section>

      <section className="bg-white border border-stone-200 rounded-lg p-5">
        <h3 className="font-semibold text-stone-800 mb-3">Actual</h3>
        {matchedActivity ? (
          <dl className="grid grid-cols-3 gap-4 text-sm">
            <div>
              <dt className="text-stone-500">Distance</dt>
              <dd className="font-medium">{mi(matchedActivity.distanceM)}</dd>
            </div>
            <div>
              <dt className="text-stone-500">Time</dt>
              <dd className="font-medium">{minSec(matchedActivity.movingSeconds)}</dd>
            </div>
            <div>
              <dt className="text-stone-500">Vert</dt>
              <dd className="font-medium">{ft(matchedActivity.elevationGainM) ?? "—"}</dd>
            </div>
            <div className="col-span-3 text-stone-500 text-xs pt-2 border-t border-stone-100">
              {matchedActivity.source} · {matchedActivity.name ?? matchedActivity.activityType}
            </div>
          </dl>
        ) : (
          <p className="text-stone-500 text-sm">No matching activity yet.</p>
        )}
      </section>
    </div>
  );
}
