import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { api, type TemplateSummary } from "../api/client";

export const Route = createFileRoute("/templates")({
  component: Templates,
});

function Templates() {
  const q = useQuery({ queryKey: ["templates"], queryFn: () => api.templates() });

  if (q.isLoading) return <p className="text-stone-500">Loading…</p>;
  if (q.error) return <p className="text-red-700">Error: {String(q.error)}</p>;
  if (!q.data) return null;

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold">Pick a training plan</h2>
        <p className="text-stone-600 mt-1">
          Choose a template, configure your race date, and we'll build the calendar for you.
          You can edit individual workouts after.
        </p>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {q.data.map((t) => (
          <TemplateCard key={t.key} t={t} />
        ))}
      </div>
    </div>
  );
}

function TemplateCard({ t }: { t: TemplateSummary }) {
  const distance =
    t.defaultDistanceM > 0 ? `${(t.defaultDistanceM / 1609.34).toFixed(1)} mi` : "—";
  const vert = t.defaultVertM > 0 ? ` · ${Math.round(t.defaultVertM * 3.281).toLocaleString()} ft vert` : "";
  return (
    <Link
      to="/templates/$key"
      params={{ key: t.key }}
      className="block bg-white border border-stone-200 rounded-lg p-5 hover:border-stone-400 hover:shadow-sm transition-all"
    >
      <div className="flex items-baseline justify-between gap-3 mb-1">
        <h3 className="font-semibold text-stone-900">{t.name}</h3>
        <span className="text-xs text-stone-500 whitespace-nowrap">{t.weeks} wk</span>
      </div>
      <div className="text-xs text-stone-500 mb-3 space-x-2">
        <span className="inline-block px-2 py-0.5 rounded bg-stone-100 ring-1 ring-stone-200">
          {t.raceCategory}
        </span>
        <span className="inline-block px-2 py-0.5 rounded bg-stone-100 ring-1 ring-stone-200">
          {t.terrain}
        </span>
        <span className="inline-block px-2 py-0.5 rounded bg-stone-100 ring-1 ring-stone-200">
          {t.level}
        </span>
      </div>
      <p className="text-sm text-stone-700 line-clamp-3">{t.description}</p>
      <p className="text-xs text-stone-500 mt-3">
        {distance}
        {vert}
      </p>
    </Link>
  );
}
