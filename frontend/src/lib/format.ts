export const mi = (m: number | null | undefined) =>
  m == null ? null : `${(m / 1609.34).toFixed(1)} mi`;

export const ft = (m: number | null | undefined) =>
  m == null ? null : `${Math.round(m * 3.281).toLocaleString()} ft`;

export const minSec = (s: number | null | undefined) => {
  if (s == null) return null;
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  if (h > 0) return `${h}h${m.toString().padStart(2, "0")}m`;
  return `${m}m`;
};
