import { Decoder, Stream } from "@garmin/fitsdk";

/** Summary of one .FIT file's primary session, normalized for our backend. */
export interface FitSummary {
  externalId: string;       // sha1 of the source bytes
  startedAt: string;        // ISO instant
  distanceM: number;
  movingSeconds: number;
  elevationGainM: number | null;
  avgHr: number | null;
  activityType: string;     // "run", "trail_run", "ride", "hike", ...
  rawSport: string | null;
  rawSubSport: string | null;
  fileName: string;
}

export async function parseFit(file: File): Promise<FitSummary> {
  const buf = await file.arrayBuffer();
  const externalId = await sha1Hex(buf);

  const stream = Stream.fromArrayBuffer(buf);
  if (!Decoder.isFIT(stream)) throw new Error("not a .FIT file");

  const decoder = new Decoder(stream);
  if (!decoder.checkIntegrity()) {
    throw new Error("file failed FIT integrity check (corrupt or truncated)");
  }

  const { messages } = decoder.read({
    applyScaleAndOffset: true,
    expandSubFields: true,
    expandComponents: true,
    convertTypesToStrings: true,
    convertDateTimesToDates: true,
    includeUnknownFields: false,
    mergeHeartRates: true,
  });

  const sessions = (messages as Record<string, unknown[]>).sessionMesgs ?? [];
  if (sessions.length === 0) throw new Error("no session message in file");

  // Use the first session — multi-sport activities have several but for our
  // purposes the first is the primary. Cast to a permissive shape; fields are
  // optional in the FIT spec so we coalesce.
  type Session = {
    startTime?: Date | string;
    totalTimerTime?: number;        // seconds
    totalElapsedTime?: number;      // seconds
    totalDistance?: number;         // meters
    totalAscent?: number;           // meters
    avgHeartRate?: number;          // bpm
    sport?: string;
    subSport?: string;
  };
  const s = sessions[0] as Session;

  const started =
    s.startTime instanceof Date
      ? s.startTime
      : new Date(s.startTime ?? Date.now());

  const distanceM = Math.round(s.totalDistance ?? 0);
  const movingSeconds = Math.round(s.totalTimerTime ?? s.totalElapsedTime ?? 0);
  const elevationGainM =
    s.totalAscent != null ? Math.round(s.totalAscent) : null;
  const avgHr = s.avgHeartRate != null ? Math.round(s.avgHeartRate) : null;

  return {
    externalId,
    startedAt: started.toISOString(),
    distanceM,
    movingSeconds,
    elevationGainM,
    avgHr,
    activityType: mapActivityType(s.sport ?? null, s.subSport ?? null),
    rawSport: s.sport ?? null,
    rawSubSport: s.subSport ?? null,
    fileName: file.name,
  };
}

function mapActivityType(sport: string | null, subSport: string | null): string {
  const sp = (sport ?? "").toLowerCase();
  const sub = (subSport ?? "").toLowerCase();
  if (sp === "running") {
    if (sub === "trail") return "trail_run";
    if (sub === "track") return "track_run";
    if (sub === "treadmill" || sub === "indoor_running") return "treadmill_run";
    return "run";
  }
  if (sp === "cycling") return "ride";
  if (sp === "hiking" || sp === "walking") return "hike";
  if (sp === "swimming") return "swim";
  return sp || "activity";
}

async function sha1Hex(buf: ArrayBuffer): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-1", buf);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}
