import { clsx } from "clsx";

const TYPE_STYLES: Record<string, string> = {
  easy: "bg-sky-100 text-sky-800 ring-sky-200",
  long: "bg-violet-100 text-violet-800 ring-violet-200",
  vert: "bg-rose-100 text-rose-800 ring-rose-200",
  tempo: "bg-amber-100 text-amber-800 ring-amber-200",
  intervals: "bg-orange-100 text-orange-800 ring-orange-200",
  recovery: "bg-emerald-100 text-emerald-800 ring-emerald-200",
  race: "bg-yellow-300 text-yellow-900 ring-yellow-400 font-bold",
  xtrain: "bg-stone-100 text-stone-700 ring-stone-200",
  rest: "bg-stone-50 text-stone-500 ring-stone-200",
};

const STATUS_STYLES: Record<string, string> = {
  completed: "bg-emerald-100 text-emerald-800",
  partial: "bg-amber-100 text-amber-800",
  modified: "bg-orange-100 text-orange-800",
  missed: "bg-rose-100 text-rose-800",
  pending: "bg-stone-100 text-stone-500",
};

export function TypeBadge({ type }: { type: string }) {
  return (
    <span
      className={clsx(
        "inline-block px-2 py-0.5 rounded text-xs uppercase tracking-wide ring-1",
        TYPE_STYLES[type] ?? TYPE_STYLES.easy,
      )}
    >
      {type}
    </span>
  );
}

export function StatusBadge({ status }: { status: string }) {
  return (
    <span
      className={clsx(
        "inline-block px-2 py-0.5 rounded text-xs",
        STATUS_STYLES[status] ?? STATUS_STYLES.pending,
      )}
    >
      {status}
    </span>
  );
}
