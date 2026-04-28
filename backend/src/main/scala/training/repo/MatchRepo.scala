package training.repo

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import training.domain.*
import training.repo.Codecs.given

final class MatchRepo(xa: Transactor[IO]):

  def find(plannedId: PlannedWorkoutId): IO[Option[WorkoutMatch]] =
    sql"""
      select planned_workout_id, activity_id, status, computed_at
      from workout_match where planned_workout_id = ${plannedId.value}
    """.query[WorkoutMatch].option.transact(xa)

  def upsert(plannedId: PlannedWorkoutId, activityId: Option[ActivityId], status: MatchStatus): IO[Int] =
    sql"""
      insert into workout_match (planned_workout_id, activity_id, status)
      values (${plannedId.value}, ${activityId.map(_.value)}, $status)
      on conflict (planned_workout_id) do update set
        activity_id = excluded.activity_id,
        status      = excluded.status,
        computed_at = now()
    """.update.run.transact(xa)

  /** Detach this activity from any planned workout it was previously matched
    * to (clears activity_id, sets status back to 'pending'). Returns count
    * of rows touched. */
  def detachActivity(activityId: ActivityId): IO[Int] =
    sql"""
      update workout_match
      set activity_id = null, status = 'pending', computed_at = now()
      where activity_id = ${activityId.value}
    """.update.run.transact(xa)

  /** What planned_workout (if any) is this activity currently matched to? */
  def findByActivity(activityId: ActivityId): IO[Option[PlannedWorkoutId]] =
    sql"""
      select planned_workout_id from workout_match
      where activity_id = ${activityId.value}
    """.query[Long].option.map(_.map(PlannedWorkoutId.apply)).transact(xa)
