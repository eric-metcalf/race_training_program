import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { format, parseISO } from "date-fns";
import { Link } from "@tanstack/react-router";
import { api } from "../api/client";
import { TypeBadge, StatusBadge } from "./WorkoutBadge";
import { WorkoutEditor } from "./WorkoutEditor";
import { mi, ft, minSec } from "../lib/format";

interface Props {
  date: string;
  onClose: () => void;
}

export function WorkoutPanel({ date, onClose }: Props) {
  const [editing, setEditing] = useState(false);
  const q = useQuery({
    queryKey: ["workout", date],
    queryFn: () => api.workout(date),
  });

  if (q.isLoading)
    return <div className="px-4 py-3 text-sm text-stone-500">Loading…</div>;
  if (q.error)
    return (
      <div className="px-4 py-3 text-sm text-red-700">Error: {String(q.error)}</div>
    );
  if (!q.data) return null;

  const { planned, matchedActivity } = q.data;

  return (
    <div className="border-t border-stone-200 bg-stone-50 px-5 py-4">
      <div className="flex items-baseline justify-between mb-3">
        <div className="flex items-center gap-3">
          <h3 className="font-semibold text-stone-900">
            {format(parseISO(planned.date), "EEEE, MMM d")}
          </h3>
          <TypeBadge type={planned.type} />
          <StatusBadge status={planned.matchStatus} />
        </div>
        <div className="flex items-center gap-3 text-sm">
          <Link
            to="/plan/$date"
            params={{ date: planned.date }}
            className="text-stone-500 hover:text-stone-800"
          >
            Open ↗
          </Link>
          <button
            onClick={onClose}
            aria-label="Close"
            className="text-stone-400 hover:text-stone-700"
          >
            ✕
          </button>
        </div>
      </div>

      {editing ? (
        <WorkoutEditor workout={planned} onDone={() => setEditing(false)} />
      ) : (
        <div className="space-y-3">
          <div className="grid grid-cols-4 gap-4 text-sm">
            <Stat label="Target distance" value={mi(planned.targetDistanceM)} />
            <Stat label="Target duration" value={minSec(planned.targetDurationS)} />
            <Stat label="Target vert"     value={ft(planned.targetVertM)} />
            <Stat label="Intensity"       value={planned.intensity ?? null} />
          </div>
          {planned.notes && (
            <p className="text-sm italic text-stone-600 border-l-2 border-stone-300 pl-3">
              {planned.notes}
            </p>
          )}

          <div className="border-t border-stone-200 pt-3">
            <p className="text-xs uppercase tracking-wide text-stone-500 mb-2">Actual</p>
            {matchedActivity ? (
              <div className="grid grid-cols-4 gap-4 text-sm">
                <Stat label="Distance" value={mi(matchedActivity.distanceM)} />
                <Stat label="Time"     value={minSec(matchedActivity.movingSeconds)} />
                <Stat label="Vert"     value={ft(matchedActivity.elevationGainM)} />
                <Stat label="Source"   value={matchedActivity.source} />
              </div>
            ) : (
              <p className="text-sm text-stone-500">No matching activity yet.</p>
            )}
          </div>

          <div className="pt-1">
            <button
              onClick={() => setEditing(true)}
              className="text-sm text-stone-700 hover:text-stone-900 underline"
            >
              Edit workout
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string | null }) {
  return (
    <div>
      <div className="text-xs text-stone-500">{label}</div>
      <div className="font-medium text-stone-900">{value ?? "—"}</div>
    </div>
  );
}
