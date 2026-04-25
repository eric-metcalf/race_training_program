import { useState, useRef, type DragEvent, type ChangeEvent } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { format, parseISO } from "date-fns";
import { clsx } from "clsx";
import { api, type CreateActivityResponse } from "../api/client";
import { parseFit, type FitSummary } from "../lib/fitParse";
import { mi, ft, minSec } from "../lib/format";
import { StatusBadge } from "../components/WorkoutBadge";

export const Route = createFileRoute("/upload")({
  component: Upload,
});

type State =
  | { kind: "idle" }
  | { kind: "parsing"; fileName: string }
  | { kind: "ready"; summary: FitSummary }
  | { kind: "error"; message: string };

function Upload() {
  const qc = useQueryClient();
  const [state, setState] = useState<State>({ kind: "idle" });
  const [imported, setImported] = useState<{
    summary: FitSummary;
    response: CreateActivityResponse;
  } | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const acceptFile = async (file: File) => {
    setImported(null);
    setState({ kind: "parsing", fileName: file.name });
    try {
      const summary = await parseFit(file);
      setState({ kind: "ready", summary });
    } catch (e) {
      setState({ kind: "error", message: e instanceof Error ? e.message : String(e) });
    }
  };

  const importMut = useMutation({
    mutationFn: (summary: FitSummary) =>
      api.createActivity({
        source: "fit",
        externalId: summary.externalId,
        startedAt: summary.startedAt,
        distanceM: summary.distanceM,
        movingSeconds: summary.movingSeconds,
        elevationGainM: summary.elevationGainM ?? undefined,
        avgHr: summary.avgHr ?? undefined,
        activityType: summary.activityType,
        name: summary.fileName,
      }),
    onSuccess: (response, summary) => {
      qc.invalidateQueries({ queryKey: ["plan"] });
      qc.invalidateQueries({ queryKey: ["workout"] });
      setImported({ summary, response });
      setState({ kind: "idle" });
    },
  });

  const onDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setDragActive(false);
    const f = e.dataTransfer.files?.[0];
    if (f) void acceptFile(f);
  };
  const onPick = (e: ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (f) void acceptFile(f);
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold">Upload .FIT activity</h2>
        <p className="text-stone-600 mt-1 text-sm">
          Drop a <code>.fit</code> file from Garmin Connect, your watch, or any other
          device that exports the standard format. The file is parsed in your browser —
          we only send the summary (start time, distance, duration, vert, HR) to the server.
        </p>
      </div>

      <div
        onDragOver={(e) => {
          e.preventDefault();
          setDragActive(true);
        }}
        onDragLeave={() => setDragActive(false)}
        onDrop={onDrop}
        onClick={() => inputRef.current?.click()}
        className={clsx(
          "border-2 border-dashed rounded-lg p-10 text-center cursor-pointer transition-colors",
          dragActive ? "border-stone-700 bg-stone-100" : "border-stone-300 bg-white hover:bg-stone-50",
        )}
      >
        <p className="text-stone-700">
          {state.kind === "parsing"
            ? `Parsing ${state.fileName}…`
            : "Drop a .fit file here, or click to choose"}
        </p>
        <input
          ref={inputRef}
          type="file"
          accept=".fit"
          onChange={onPick}
          className="hidden"
        />
      </div>

      {state.kind === "error" && (
        <div className="bg-red-50 border border-red-200 rounded p-3 text-sm text-red-800">
          Couldn't parse: {state.message}
        </div>
      )}

      {state.kind === "ready" && (
        <SummaryCard
          summary={state.summary}
          onImport={() => importMut.mutate(state.summary)}
          onCancel={() => setState({ kind: "idle" })}
          pending={importMut.isPending}
          error={importMut.error ? String(importMut.error) : null}
        />
      )}

      {imported && <ImportResult imported={imported} />}
    </div>
  );
}

function SummaryCard({
  summary,
  onImport,
  onCancel,
  pending,
  error,
}: {
  summary: FitSummary;
  onImport: () => void;
  onCancel: () => void;
  pending: boolean;
  error: string | null;
}) {
  const dt = parseISO(summary.startedAt);
  return (
    <section className="bg-white border border-stone-200 rounded-lg p-5 space-y-3">
      <div className="flex items-baseline justify-between">
        <h3 className="font-semibold text-stone-900">{summary.fileName}</h3>
        <span className="text-xs uppercase tracking-wide text-stone-500">
          {summary.activityType}
        </span>
      </div>
      <div className="text-sm text-stone-600">
        {format(dt, "EEEE, MMM d, yyyy 'at' h:mm a")}
      </div>
      <dl className="grid grid-cols-4 gap-4 text-sm pt-1">
        <Stat label="Distance" value={mi(summary.distanceM)} />
        <Stat label="Moving"   value={minSec(summary.movingSeconds)} />
        <Stat label="Vert"     value={ft(summary.elevationGainM)} />
        <Stat label="Avg HR"   value={summary.avgHr != null ? `${summary.avgHr} bpm` : null} />
      </dl>
      <div className="flex gap-2 pt-2">
        <button
          onClick={onImport}
          disabled={pending}
          className="bg-stone-800 text-white text-sm px-4 py-2 rounded hover:bg-stone-900 disabled:opacity-50"
        >
          {pending ? "Importing…" : "Import this activity"}
        </button>
        <button
          onClick={onCancel}
          className="text-sm text-stone-500 hover:text-stone-800 px-3 py-2"
        >
          Cancel
        </button>
        {error && <span className="text-sm text-red-700 ml-auto">{error}</span>}
      </div>
      <p className="text-xs text-stone-400">
        Dedupe key (SHA-1): <span className="font-mono">{summary.externalId.slice(0, 16)}…</span>
      </p>
    </section>
  );
}

function ImportResult({
  imported,
}: {
  imported: { summary: FitSummary; response: CreateActivityResponse };
}) {
  const { summary, response } = imported;
  const dt = parseISO(summary.startedAt);
  const date = format(dt, "yyyy-MM-dd");
  return (
    <section className="bg-emerald-50 border border-emerald-200 rounded-lg p-4 text-sm space-y-2">
      <div className="flex items-baseline justify-between">
        <p className="font-medium text-emerald-900">
          {response.duplicate ? "Already imported." : "Imported!"}
        </p>
        {response.matchStatus && <StatusBadge status={response.matchStatus} />}
      </div>
      {response.matchedPlannedWorkoutId ? (
        <p className="text-emerald-800">
          Matched the planned workout on{" "}
          <Link
            to="/plan/$date"
            params={{ date }}
            className="underline hover:no-underline"
          >
            {format(dt, "EEEE, MMM d")}
          </Link>
          .
        </p>
      ) : (
        <p className="text-stone-700">
          No planned workout on {format(dt, "EEEE, MMM d")} to match against — saved as a
          standalone activity.
        </p>
      )}
    </section>
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
