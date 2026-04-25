package training.repo

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import training.domain.*
import training.repo.Codecs.given

import java.time.LocalDate

final class PlanRepo(xa: Transactor[IO]):

  /** Fetch the unique plan for a (race, athlete) pair. */
  def findPlan(raceId: RaceId, athleteId: AthleteId): IO[Option[TrainingPlan]] =
    sql"""
      select id, race_id, athlete_id, template_key, generated_at
      from training_plan
      where race_id = ${raceId.value} and athlete_id = ${athleteId.value}
    """.query[TrainingPlan].option.transact(xa)

  def findPlanById(planId: PlanId): IO[Option[TrainingPlan]] =
    sql"""
      select id, race_id, athlete_id, template_key, generated_at
      from training_plan where id = ${planId.value}
    """.query[TrainingPlan].option.transact(xa)

  /** Join of training_plan + race + workout count, one row per plan. */
  def listWithRaces(athleteId: AthleteId): IO[List[PlanRepo.PlanWithRace]] =
    sql"""
      select
        tp.id, tp.race_id, tp.template_key, tp.generated_at,
        r.name, r.race_date, r.distance_m, r.vert_m, r.location,
        (select count(*) from planned_workout pw where pw.plan_id = tp.id) as wc
      from training_plan tp
      join race r on r.id = tp.race_id
      where tp.athlete_id = ${athleteId.value}
      order by r.race_date asc
    """.query[PlanRepo.PlanWithRace].to[List].transact(xa)

  /** Insert a fresh plan row. */
  def insertPlan(raceId: RaceId, athleteId: AthleteId, templateKey: String): IO[PlanId] =
    sql"""
      insert into training_plan (race_id, athlete_id, template_key)
      values (${raceId.value}, ${athleteId.value}, $templateKey)
    """.update.withUniqueGeneratedKeys[Long]("id").map(PlanId.apply).transact(xa)

  /** Delete all planned workouts for a plan (used by regeneration). */
  def clearWorkouts(planId: PlanId): IO[Int] =
    sql"delete from planned_workout where plan_id = ${planId.value}".update.run.transact(xa)

  /** Insert many planned workouts in one batch. */
  def insertWorkouts(rows: List[PlannedWorkoutInsert]): IO[Int] =
    val sql = """
      insert into planned_workout
        (plan_id, workout_date, workout_type, target_distance_m, target_duration_s,
         target_vert_m, intensity, notes)
      values (?, ?, ?, ?, ?, ?, ?, ?)
    """
    Update[PlannedWorkoutInsert](sql).updateMany(rows).transact(xa)

  /** List planned workouts in [from, to] inclusive (date range). */
  def listInRange(planId: PlanId, from: LocalDate, to: LocalDate): IO[List[PlannedWorkout]] =
    sql"""
      select id, plan_id, workout_date, workout_type,
             target_distance_m, target_duration_s, target_vert_m, intensity, notes
      from planned_workout
      where plan_id = ${planId.value} and workout_date between $from and $to
      order by workout_date
    """.query[PlannedWorkout].to[List].transact(xa)

  def findByDate(planId: PlanId, date: LocalDate): IO[Option[PlannedWorkout]] =
    sql"""
      select id, plan_id, workout_date, workout_type,
             target_distance_m, target_duration_s, target_vert_m, intensity, notes
      from planned_workout
      where plan_id = ${planId.value} and workout_date = $date
    """.query[PlannedWorkout].option.transact(xa)

  def findById(id: PlannedWorkoutId): IO[Option[PlannedWorkout]] =
    sql"""
      select id, plan_id, workout_date, workout_type,
             target_distance_m, target_duration_s, target_vert_m, intensity, notes
      from planned_workout
      where id = ${id.value}
    """.query[PlannedWorkout].option.transact(xa)

  /** Update the editable fields of a planned workout. Returns row count (0 or 1). */
  def updateWorkout(
      id: PlannedWorkoutId,
      workoutType: WorkoutType,
      targetDistanceM: Option[Int],
      targetDurationS: Option[Int],
      targetVertM: Option[Int],
      intensity: Option[String],
      notes: Option[String]
  ): IO[Int] =
    sql"""
      update planned_workout set
        workout_type      = $workoutType,
        target_distance_m = $targetDistanceM,
        target_duration_s = $targetDurationS,
        target_vert_m     = $targetVertM,
        intensity         = $intensity,
        notes             = $notes
      where id = ${id.value}
    """.update.run.transact(xa)

object PlanRepo:
  /** Tuple-shaped row for batch insert (matches the SQL VALUES order). */
  type PlannedWorkoutInsert = (
      Long,           // plan_id
      LocalDate,      // workout_date
      WorkoutType,    // workout_type
      Option[Int],    // target_distance_m
      Option[Int],    // target_duration_s
      Option[Int],    // target_vert_m
      Option[String], // intensity
      Option[String]  // notes
  )

  import java.time.{Instant, LocalDate as JLocalDate}

  /** Plan + its race, for the plans list view. */
  final case class PlanWithRace(
      planId: PlanId,
      raceId: RaceId,
      templateKey: String,
      generatedAt: Instant,
      raceName: String,
      raceDate: JLocalDate,
      distanceM: Int,
      vertM: Int,
      location: Option[String],
      workoutCount: Int
  )

export PlanRepo.PlannedWorkoutInsert
