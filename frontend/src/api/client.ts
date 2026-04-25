// Typed API client. Types come from the OpenAPI spec (`make regen-api` to refresh).

import type { components, paths } from "./generated";

export type Race = components["schemas"]["Race"];
export type PlannedWorkoutView = components["schemas"]["PlannedWorkoutView"];
export type ActivityView = components["schemas"]["ActivityView"];
export type WorkoutDetailView = components["schemas"]["WorkoutDetailView"];
export type RegenerateResponse = components["schemas"]["RegenerateResponse"];
export type TemplateSummary = components["schemas"]["TemplateSummary"];
export type TemplateDetailView = components["schemas"]["TemplateDetailView"];
export type TemplateWorkoutView = components["schemas"]["TemplateWorkoutView"];
export type CreatePlanRequest = components["schemas"]["CreatePlanRequest"];
export type CreatePlanResponse = components["schemas"]["CreatePlanResponse"];
export type CreateActivityRequest = components["schemas"]["CreateActivityRequest"];
export type CreateActivityResponse = components["schemas"]["CreateActivityResponse"];
export type PlanSummaryView = components["schemas"]["PlanSummaryView"];

async function jsonGet<T>(path: string): Promise<T> {
  const r = await fetch(path);
  if (!r.ok) throw new Error(`${path} → ${r.status}`);
  return (await r.json()) as T;
}

async function jsonPost<T>(path: string): Promise<T> {
  const r = await fetch(path, { method: "POST" });
  if (!r.ok) throw new Error(`${path} → ${r.status}`);
  return (await r.json()) as T;
}

async function jsonPut<TBody, TResp>(path: string, body: TBody): Promise<TResp> {
  const r = await fetch(path, {
    method: "PUT",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!r.ok) throw new Error(`${path} → ${r.status}`);
  return (await r.json()) as TResp;
}

export interface WorkoutEdit {
  type: string;
  targetDistanceM: number | null;
  targetDurationS: number | null;
  targetVertM: number | null;
  intensity: string | null;
  notes: string | null;
}

export const api = {
  race: () => jsonGet<Race>("/api/race/current"),

  plan: (from: string, to: string) =>
    jsonGet<PlannedWorkoutView[]>(`/api/plan?from=${from}&to=${to}`),

  workout: (date: string) => jsonGet<WorkoutDetailView>(`/api/plan/${date}`),

  regenerate: () => jsonPost<RegenerateResponse>("/api/plan/regenerate"),

  activities: (limit = 50) => jsonGet<ActivityView[]>(`/api/activities?limit=${limit}`),

  updateWorkout: (id: number, edit: WorkoutEdit) =>
    jsonPut<WorkoutEdit, PlannedWorkoutView>(`/api/plan/${id}`, edit),

  templates: () => jsonGet<TemplateSummary[]>("/api/templates"),

  template: (key: string) => jsonGet<TemplateDetailView>(`/api/templates/${key}`),

  createPlan: (req: CreatePlanRequest) =>
    jsonPostBody<CreatePlanRequest, CreatePlanResponse>("/api/plans", req),

  createActivity: (req: CreateActivityRequest) =>
    jsonPostBody<CreateActivityRequest, CreateActivityResponse>("/api/activities", req),

  plans: () => jsonGet<PlanSummaryView[]>("/api/plans"),

  activatePlan: (id: number) => jsonPost<{ ok: boolean }>(`/api/plans/${id}/activate`),

  deletePlan: (id: number) =>
    fetch(`/api/plans/${id}`, { method: "DELETE" }).then((r) => {
      if (!r.ok) throw new Error(`/api/plans/${id} → ${r.status}`);
      return r.json() as Promise<{ ok: boolean }>;
    }),
};

async function jsonPostBody<TBody, TResp>(path: string, body: TBody): Promise<TResp> {
  const r = await fetch(path, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!r.ok) throw new Error(`${path} → ${r.status}`);
  return (await r.json()) as TResp;
}

// Don't fail if generated.ts hasn't been built yet (`paths` is unused — type-only export).
export type Paths = paths;
