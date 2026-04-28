package training.repo

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import training.domain.*
import training.repo.Codecs.given

import java.time.{Instant, LocalDate, ZoneOffset}

final class ActivityRepo(xa: Transactor[IO]):

  /** Insert; on (source, external_id) conflict, update mutable fields.
    *
    * Returns `(id, wasNew)` where `wasNew = true` when this call inserted a
    * brand-new row (Postgres trick: `xmax = 0` on the returned row).
    */
  def upsert(row: ActivityInsert): IO[(ActivityId, Boolean)] =
    sql"""
      insert into activity
        (athlete_id, source, external_id, started_at, distance_m, moving_seconds,
         elevation_gain_m, avg_hr, activity_type, name, raw)
      values (${row.athleteId.value}, ${row.source}, ${row.externalId}, ${row.startedAt},
              ${row.distanceM}, ${row.movingSeconds}, ${row.elevationGainM}, ${row.avgHr},
              ${row.activityType}, ${row.name}, ${row.rawJson}::jsonb)
      on conflict (source, external_id) do update set
        started_at      = excluded.started_at,
        distance_m      = excluded.distance_m,
        moving_seconds  = excluded.moving_seconds,
        elevation_gain_m= excluded.elevation_gain_m,
        avg_hr          = excluded.avg_hr,
        activity_type   = excluded.activity_type,
        name            = excluded.name,
        raw             = excluded.raw
      returning id, (xmax = 0) as inserted
    """.query[(Long, Boolean)].unique.map((id, ins) => (ActivityId(id), ins)).transact(xa)

  def findById(id: ActivityId): IO[Option[Activity]] =
    sql"""
      select id, athlete_id, source, external_id, started_at, distance_m,
             moving_seconds, elevation_gain_m, avg_hr, activity_type, name
      from activity where id = ${id.value}
    """.query[Activity].option.transact(xa)

  def list(limit: Int): IO[List[Activity]] =
    sql"""
      select id, athlete_id, source, external_id, started_at, distance_m,
             moving_seconds, elevation_gain_m, avg_hr, activity_type, name
      from activity
      order by started_at desc
      limit $limit
    """.query[Activity].to[List].transact(xa)

  /** Find activities started in [date-1, date+1] of a given type prefix (case-insensitive). */
  def findCandidates(athleteId: AthleteId, date: LocalDate): IO[List[Activity]] =
    val from = date.minusDays(1).atStartOfDay.toInstant(ZoneOffset.UTC)
    val to   = date.plusDays(2).atStartOfDay.toInstant(ZoneOffset.UTC)
    sql"""
      select id, athlete_id, source, external_id, started_at, distance_m,
             moving_seconds, elevation_gain_m, avg_hr, activity_type, name
      from activity
      where athlete_id = ${athleteId.value}
        and started_at >= $from and started_at < $to
      order by started_at
    """.query[Activity].to[List].transact(xa)

object ActivityRepo:
  final case class ActivityInsert(
      athleteId: AthleteId,
      source: ActivitySource,
      externalId: String,
      startedAt: Instant,
      distanceM: Int,
      movingSeconds: Int,
      elevationGainM: Option[Int],
      avgHr: Option[Int],
      activityType: String,
      name: Option[String],
      rawJson: String // serialized JSON; cast to jsonb in SQL
  )

export ActivityRepo.ActivityInsert
