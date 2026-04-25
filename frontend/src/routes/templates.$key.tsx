import { useEffect, useState } from "react";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { addWeeks, format, parseISO, differenceInCalendarWeeks } from "date-fns";
import { api, type TemplateWorkoutView } from "../api/client";
import { TypeBadge } from "../components/WorkoutBadge";
import { mi, ft, minSec } from "../lib/format";

export const Route = createFileRoute("/templates/$key")({
  component: TemplateDetail,
});

function TemplateDetail() {
  const { key } = Route.useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const q = useQuery({ queryKey: ["template", key], queryFn: () => api.template(key) });

  const [raceName, setRaceName] = useState("");
  const [raceDate, setRaceDate] = useState("");
  const [location, setLocation] = useState("");

  // Initialize the form from the template once it loads.
  useEffect(() => {
    if (q.data && raceName === "" && raceDate === "") {
      setRaceName(q.data.summary.name.split(" — ")[0] || q.data.summary.name);
      setRaceDate(format(addWeeks(new Date(), q.data.summary.weeks), "yyyy-MM-dd"));
    }
  }, [q.data, raceName, raceDate]);

  const create = useMutation({
    mutationFn: () =>
      api.createPlan({
        templateKey: key,
        raceName,
        raceDate,
        location: location.trim() === "" ? undefined : location.trim(),
        distanceM: undefined,
        vertM: undefined,
        notes: undefined,
      }),
    onSuccess: () => {
      qc.invalidateQueries();
      navigate({ to: "/plan" });
    },
  });

  if (q.isLoading) return <p className="text-stone-500">Loading…</p>;
  if (q.error) return <p className="text-red-700">Error: {String(q.error)}</p>;
  if (!q.data) return null;

  const { summary } = q.data;
  const workouts = q.data.workouts ?? [];
  const weeksUntil = raceDate
    ? differenceInCalendarWeeks(parseISO(raceDate), new Date(), { weekStartsOn: 1 })
    : 0;
  const tooShort = !!(raceDate && weeksUntil < summary.weeks);

  const distance = summary.defaultDistanceM > 0
    ? `${(summary.defaultDistanceM / 1609.34).toFixed(1)} mi`
    : null;

  return (
    <div className="space-y-6">
      <div>
        <Link to="/templates" className="text-sm text-stone-500 hover:text-stone-800">
          ← All templates
        </Link>
        <h2 className="text-2xl font-bold mt-1">{summary.name}</h2>
        <p className="text-stone-600 mt-1">{summary.description}</p>
        <div className="text-xs text-stone-500 mt-2 space-x-2">
          <Pill>{summary.raceCategory}</Pill>
          <Pill>{summary.terrain}</Pill>
          <Pill>{summary.level}</Pill>
          <Pill>{summary.weeks} weeks</Pill>
          {distance && <Pill>{distance}</Pill>}
        </div>
      </div>

      <section className="bg-white border border-stone-200 rounded-lg p-5">
        <h3 className="font-semibold text-stone-900 mb-3">Configure your race</h3>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            create.mutate();
          }}
          className="space-y-3"
        >
          <Field label="Race name">
            <input
              type="text"
              value={raceName}
              onChange={(e) => setRaceName(e.target.value)}
              required
              className="w-full border border-stone-300 rounded px-3 py-2 text-sm"
            />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Race date">
              <input
                type="date"
                value={raceDate}
                onChange={(e) => setRaceDate(e.target.value)}
                required
                className="w-full border border-stone-300 rounded px-3 py-2 text-sm"
              />
            </Field>
            <Field label="Location (optional)">
              <input
                type="text"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                placeholder="e.g. Boston, MA"
                className="w-full border border-stone-300 rounded px-3 py-2 text-sm"
              />
            </Field>
          </div>
          {tooShort && (
            <p className="text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded px-3 py-2">
              Heads up: race is in {weeksUntil} weeks but this template is {summary.weeks} weeks.
              Earlier-week workouts will land in the past — you can ignore them or trim them later.
            </p>
          )}
          <div className="flex items-center gap-2 pt-1">
            <button
              type="submit"
              disabled={create.isPending || !raceName || !raceDate}
              className="bg-stone-800 text-white text-sm px-4 py-2 rounded hover:bg-stone-900 disabled:opacity-50"
            >
              {create.isPending ? "Building…" : "Build my plan"}
            </button>
            {create.error && (
              <span className="text-sm text-red-700">Error: {String(create.error)}</span>
            )}
          </div>
        </form>
      </section>

      <section className="bg-white border border-stone-200 rounded-lg overflow-hidden">
        <header className="px-4 py-2 border-b border-stone-100 bg-stone-50 text-xs uppercase tracking-wide text-stone-500">
          Workout preview ({workouts.length} sessions across {summary.weeks} weeks)
        </header>
        <WorkoutPreview workouts={workouts} weeks={summary.weeks} />
      </section>
    </div>
  );
}

function WorkoutPreview({ workouts, weeks }: { workouts: TemplateWorkoutView[]; weeks: number }) {
  const byWeek = new Map<number, TemplateWorkoutView[]>();
  for (const w of workouts) {
    if (!byWeek.has(w.weeksOut)) byWeek.set(w.weeksOut, []);
    byWeek.get(w.weeksOut)!.push(w);
  }
  const ordered = Array.from(byWeek.keys()).sort((a, b) => b - a);

  return (
    <div className="divide-y divide-stone-100">
      {ordered.map((wk) => (
        <WeekRow key={wk} wk={wk} weeks={weeks} dayWorkouts={byWeek.get(wk) ?? []} />
      ))}
    </div>
  );
}

function WeekRow(props: {
  wk: number;
  weeks: number;
  dayWorkouts: TemplateWorkoutView[];
}) {
  const { wk, weeks, dayWorkouts } = props;
  const label = wk === 0 ? "Race week" : `Week ${weeks - wk} (${wk} to go)`;
  return (
    <div className="px-4 py-3">
      <div className="text-xs text-stone-500 mb-1">{label}</div>
      <div className="grid grid-cols-7 gap-2 text-xs">
        {[1, 2, 3, 4, 5, 6, 7].map((dow) => (
          <DayPreview
            key={dow}
            dow={dow}
            w={dayWorkouts.find((x) => x.dayOfWeek === dow)}
          />
        ))}
      </div>
    </div>
  );
}

// One day cell in the week-by-week preview grid.
function DayPreview({ dow, w }: { dow: number; w: TemplateWorkoutView | undefined }) {
  const dowLabel = ["M", "T", "W", "T", "F", "S", "S"][dow - 1];
  if (!w) {
    return (
      <div className="min-h-[3.5rem]">
        <div className="text-stone-400 mb-0.5">{dowLabel}</div>
        <span className="text-stone-300">—</span>
      </div>
    );
  }
  const primary = mi(w.targetDistanceM) ?? minSec(w.targetDurationS) ?? "—";
  return (
    <div className="min-h-[3.5rem]">
      <div className="text-stone-400 mb-0.5">{dowLabel}</div>
      <div className="space-y-0.5">
        <TypeBadge type={w.type} />
        <div className="text-stone-700 text-[11px]">{primary}</div>
        {w.targetVertM != null && w.targetVertM > 0 && (
          <div className="text-stone-500 text-[10px]">↑ {ft(w.targetVertM)}</div>
        )}
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="block text-xs text-stone-500 mb-1">{label}</span>
      {children}
    </label>
  );
}

function Pill({ children }: { children: React.ReactNode }) {
  return (
    <span className="inline-block px-2 py-0.5 rounded bg-stone-100 ring-1 ring-stone-200">
      {children}
    </span>
  );
}
