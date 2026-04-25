-- Track which training_plan is "active" for each athlete. The dashboard,
-- calendar, and edit endpoints all read from this plan in v1.5.

alter table athlete
  add column active_plan_id bigint null
    references training_plan(id) on delete set null;

-- Backfill: if an athlete already has any plans (from v1), pick the most
-- recently generated one as active. New athletes start with null.
update athlete a set active_plan_id = (
  select id from training_plan p
  where p.athlete_id = a.id
  order by generated_at desc
  limit 1
);
