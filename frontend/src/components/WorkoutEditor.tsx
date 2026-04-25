import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api, type PlannedWorkoutView, type WorkoutEdit } from "../api/client";

const WORKOUT_TYPES = [
  "easy",
  "long",
  "vert",
  "tempo",
  "intervals",
  "recovery",
  "race",
  "xtrain",
  "rest",
] as const;

interface Props {
  workout: PlannedWorkoutView;
  onDone: () => void;
}

export function WorkoutEditor({ workout, onDone }: Props) {
  const qc = useQueryClient();

  const [type, setType] = useState(workout.type);
  const [distanceMi, setDistanceMi] = useState(metersToMi(workout.targetDistanceM));
  const [durationMin, setDurationMin] = useState(secToMin(workout.targetDurationS));
  const [vertFt, setVertFt] = useState(metersToFt(workout.targetVertM));
  const [intensity, setIntensity] = useState(workout.intensity ?? "");
  const [notes, setNotes] = useState(workout.notes ?? "");

  const save = useMutation({
    mutationFn: () => {
      const edit: WorkoutEdit = {
        type,
        targetDistanceM: distanceMi.trim() === "" ? null : Math.round(parseFloat(distanceMi) * 1609.34),
        targetDurationS: durationMin.trim() === "" ? null : Math.round(parseFloat(durationMin) * 60),
        targetVertM:     vertFt.trim()     === "" ? null : Math.round(parseFloat(vertFt) * 0.3048),
        intensity: intensity.trim() === "" ? null : intensity.trim(),
        notes:     notes.trim()     === "" ? null : notes.trim(),
      };
      return api.updateWorkout(workout.id, edit);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["plan"] });
      qc.invalidateQueries({ queryKey: ["workout", workout.date] });
      onDone();
    },
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        save.mutate();
      }}
      className="space-y-3"
    >
      <div className="grid grid-cols-4 gap-3">
        <Field label="Type">
          <select
            value={type}
            onChange={(e) => setType(e.target.value)}
            className="w-full border border-stone-300 rounded px-2 py-1 text-sm"
          >
            {WORKOUT_TYPES.map((t) => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
        </Field>
        <Field label="Distance (mi)">
          <NumInput value={distanceMi} onChange={setDistanceMi} step="0.1" />
        </Field>
        <Field label="Duration (min)">
          <NumInput value={durationMin} onChange={setDurationMin} step="1" />
        </Field>
        <Field label="Vert (ft)">
          <NumInput value={vertFt} onChange={setVertFt} step="100" />
        </Field>
      </div>
      <Field label="Intensity">
        <input
          type="text"
          value={intensity}
          onChange={(e) => setIntensity(e.target.value)}
          className="w-full border border-stone-300 rounded px-2 py-1 text-sm"
        />
      </Field>
      <Field label="Notes">
        <textarea
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          rows={3}
          className="w-full border border-stone-300 rounded px-2 py-1 text-sm"
        />
      </Field>
      <div className="flex gap-2">
        <button
          type="submit"
          disabled={save.isPending}
          className="bg-stone-800 text-white text-sm px-3 py-1 rounded hover:bg-stone-900 disabled:opacity-50"
        >
          {save.isPending ? "Saving…" : "Save"}
        </button>
        <button
          type="button"
          onClick={onDone}
          className="text-sm text-stone-500 hover:text-stone-800 px-3 py-1"
        >
          Cancel
        </button>
        {save.error && (
          <span className="text-sm text-red-700 ml-auto">Error: {String(save.error)}</span>
        )}
      </div>
    </form>
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

function NumInput({
  value,
  onChange,
  step,
}: {
  value: string;
  onChange: (s: string) => void;
  step?: string;
}) {
  return (
    <input
      type="number"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      step={step}
      className="w-full border border-stone-300 rounded px-2 py-1 text-sm"
    />
  );
}

function metersToMi(m: number | null | undefined): string {
  return m == null ? "" : (m / 1609.34).toFixed(1);
}
function metersToFt(m: number | null | undefined): string {
  return m == null ? "" : Math.round(m * 3.281).toString();
}
function secToMin(s: number | null | undefined): string {
  return s == null ? "" : Math.round(s / 60).toString();
}
