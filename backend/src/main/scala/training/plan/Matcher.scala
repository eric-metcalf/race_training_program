package training.plan

import cats.effect.IO
import cats.syntax.all.*
import training.domain.*
import training.repo.{ActivityRepo, MatchRepo, PlanRepo}

import java.time.LocalDate

/** Reconcile a planned_workout against actual activities in [date-1, date+1].
  *
  * Status:
  *   completed — distance and duration both within ±tolerance of target
  *   partial   — one within tolerance, one off
  *   modified  — same-window activity exists but neither metric in range
  *   missed    — no candidate activity, and the planned date is at least 2 days past
  *   pending   — planned date is today/future, no activity yet
  */
object Matcher:

  private val Tolerance = 0.20

  def matchOne(
      activityRepo: ActivityRepo,
      matchRepo: MatchRepo,
      athleteId: AthleteId,
      planned: PlannedWorkout,
      today: LocalDate
  ): IO[MatchStatus] =
    if planned.workoutType == WorkoutType.Rest || planned.workoutType == WorkoutType.XTrain then
      // Rest/cross-train aren't reconciled against runs in v1.
      matchRepo.upsert(planned.id, None, MatchStatus.Pending).as(MatchStatus.Pending)
    else
      activityRepo.findCandidates(athleteId, planned.date).flatMap { candidates =>
        val runs = candidates.filter(a => isRunLike(a.activityType))
        val pick = runs match
          case Nil => None
          case xs  => Some(xs.minBy(score(planned, _)))
        val status = pick match
          case Some(a) => classify(planned, a)
          case None    =>
            if today.isAfter(planned.date.plusDays(1)) then MatchStatus.Missed
            else MatchStatus.Pending
        matchRepo.upsert(planned.id, pick.map(_.id), status).as(status)
      }

  private def isRunLike(t: String): Boolean =
    val tt = t.toLowerCase
    tt.contains("run") || tt.contains("trail")

  private def score(p: PlannedWorkout, a: Activity): Double =
    val distScore = p.targetDistanceM.fold(0.0)(t => math.abs(a.distanceM - t).toDouble / t)
    val durScore  = p.targetDurationS.fold(0.0)(t => math.abs(a.movingSeconds - t).toDouble / t)
    distScore + durScore

  private def within(actual: Int, target: Int): Boolean =
    val ratio = math.abs(actual - target).toDouble / target
    ratio <= Tolerance

  private def classify(p: PlannedWorkout, a: Activity): MatchStatus =
    val distOk = p.targetDistanceM.forall(t => within(a.distanceM, t))
    val durOk  = p.targetDurationS.forall(t => within(a.movingSeconds, t))
    if p.targetDistanceM.isEmpty && p.targetDurationS.isEmpty then MatchStatus.Completed
    else (distOk, durOk) match
      case (true, true)   => MatchStatus.Completed
      case (true, false)  => MatchStatus.Partial
      case (false, true)  => MatchStatus.Partial
      case (false, false) => MatchStatus.Modified

  /** Recompute matches for every planned workout in [date - 7d, date + 1d]. */
  def matchTrailingWeek(
      planRepo: PlanRepo,
      activityRepo: ActivityRepo,
      matchRepo: MatchRepo,
      planId: PlanId,
      athleteId: AthleteId,
      today: LocalDate
  ): IO[Int] =
    planRepo.listInRange(planId, today.minusDays(7), today.plusDays(1)).flatMap { rows =>
      rows.traverse(p => matchOne(activityRepo, matchRepo, athleteId, p, today)).map(_.size)
    }
