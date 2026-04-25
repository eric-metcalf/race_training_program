-- Race Training v1 schema
--
-- Single-user (athletes table holds one row in v1 but is shaped for multi later).
-- All distances in meters, durations in seconds, elevation in meters. Display
-- conversion (mi/ft) happens on the frontend.

create table athlete (
  id              bigserial primary key,
  display_name    text        not null,
  created_at      timestamptz not null default now()
);

create table strava_tokens (
  athlete_id      bigint      primary key references athlete(id) on delete cascade,
  access_token    text        not null,
  refresh_token   text        not null,
  expires_at      timestamptz not null,
  scope           text        not null,
  athlete_strava_id bigint    null,
  updated_at      timestamptz not null default now()
);

create table race (
  id              bigserial primary key,
  name            text        not null,
  race_date       date        not null,
  distance_m      integer     not null,
  vert_m          integer     not null,
  location        text        null,
  notes           text        null
);

create table training_plan (
  id              bigserial primary key,
  race_id         bigint      not null references race(id) on delete cascade,
  athlete_id      bigint      not null references athlete(id) on delete cascade,
  template_key    text        not null,
  generated_at    timestamptz not null default now(),
  unique (athlete_id, race_id)
);

-- type enum, kept as text + check constraint for portability with doobie
create table planned_workout (
  id                  bigserial primary key,
  plan_id             bigint      not null references training_plan(id) on delete cascade,
  workout_date        date        not null,
  workout_type        text        not null
                        check (workout_type in
                          ('easy','long','vert','tempo','intervals','recovery','race','xtrain','rest')),
  target_distance_m   integer     null,
  target_duration_s   integer     null,
  target_vert_m       integer     null,
  intensity           text        null,
  notes               text        null,
  unique (plan_id, workout_date)
);
create index planned_workout_date_idx on planned_workout(workout_date);

create table activity (
  id              bigserial primary key,
  athlete_id      bigint      not null references athlete(id) on delete cascade,
  source          text        not null check (source in ('strava','fit')),
  external_id     text        not null,
  started_at      timestamptz not null,
  distance_m      integer     not null,
  moving_seconds  integer     not null,
  elevation_gain_m integer    null,
  avg_hr          integer     null,
  activity_type   text        not null,
  name            text        null,
  raw             jsonb       null,
  inserted_at     timestamptz not null default now(),
  unique (source, external_id)
);
create index activity_started_at_idx on activity(started_at);
create index activity_athlete_idx    on activity(athlete_id, started_at);

create table workout_match (
  planned_workout_id bigint     primary key references planned_workout(id) on delete cascade,
  activity_id     bigint      null references activity(id) on delete set null,
  status          text        not null
                    check (status in ('completed','partial','modified','missed','pending')),
  computed_at     timestamptz not null default now()
);

-- Seed: single athlete + Leadville Trail Marathon
insert into athlete (id, display_name) values (1, 'Eric')
  on conflict do nothing;
select setval('athlete_id_seq', greatest((select max(id) from athlete), 1));

insert into race (id, name, race_date, distance_m, vert_m, location, notes) values
  (1, 'Leadville Trail Marathon', '2026-06-27', 42500, 1830,
   'Leadville, CO',
   'Out-and-back; high point Mosquito Pass (13,185 ft / 4019 m). Highest trail marathon in the US.')
  on conflict do nothing;
select setval('race_id_seq', greatest((select max(id) from race), 1));
