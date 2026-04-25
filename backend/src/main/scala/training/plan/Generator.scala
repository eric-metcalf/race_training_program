package training.plan

import cats.effect.IO
import training.domain.*
import training.plan.PlanTemplate.TemplateDay
import training.repo.PlanRepo

import java.time.{DayOfWeek, LocalDate}

object Generator:

  /** Expand a TemplateDay list against a race date into PlannedWorkoutInsert rows.
    *
    * weeksOut convention: 0 = race week (the calendar week containing race day),
    * higher = earlier weeks. Week starts on Monday.
    */
  def expand(
      planId: PlanId,
      raceDate: LocalDate,
      template: List[TemplateDay]
  ): List[PlanRepo.PlannedWorkoutInsert] =
    val raceWeekMonday = raceDate.`with`(DayOfWeek.MONDAY)
    template.map { td =>
      val weekMonday = raceWeekMonday.minusWeeks(td.weeksOut.toLong)
      val date       = weekMonday.plusDays((td.dayOfWeek.getValue - 1).toLong)
      (
        planId.value,
        date,
        td.workoutType,
        td.targetDistanceM,
        td.targetDurationS,
        td.targetVertM,
        td.intensity,
        td.notes
      )
    }

  /** Idempotent regenerate: drop existing planned_workout rows for the plan and
    * insert the freshly expanded ones. Returns count inserted.
    */
  def regenerate(
      planRepo: PlanRepo,
      planId: PlanId,
      raceDate: LocalDate,
      template: List[TemplateDay]
  ): IO[Int] =
    for
      _ <- planRepo.clearWorkouts(planId)
      n <- planRepo.insertWorkouts(expand(planId, raceDate, template))
    yield n
