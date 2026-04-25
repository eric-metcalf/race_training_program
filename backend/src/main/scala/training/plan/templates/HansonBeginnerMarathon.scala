package training.plan.templates

import training.domain.WorkoutType
import training.plan.PlanTemplate
import training.plan.PlanTemplate.{TemplateDay, easy, mi, min, rest}

import java.time.DayOfWeek.*

/** Hanson Marathon Method — Beginner (18 weeks).
  *
  * Six runs per week. The signature move is the 16-mile cap on the long run —
  * relying on cumulative weekly fatigue (60+ mpw at peak) instead of one
  * marathon-distance long run. Speed (T) and strength (Th) workouts during
  * the build, marathon-pace work in the final block.
  *
  * Approximation of the published plan from "Hanson Marathon Method" (Luke
  * Humphrey). Workout descriptions paraphrased for personal-use within this app.
  */
object HansonBeginnerMarathon extends PlanTemplate:

  val key              = "hanson-beginner-marathon-18wk"
  val name             = "Hanson Marathon Method — Beginner (18 wk)"
  val description      =
    "Six days of running with the long run capped at 16 miles. Builds resilience " +
    "through cumulative fatigue rather than overlong long runs. Two quality days " +
    "(speed/strength) plus a marathon-pace tempo. Best for runners who can already " +
    "comfortably hold ~25 mpw."
  val raceCategory     = "Marathon"
  val terrain          = "Road"
  val level            = "Intermediate"
  val weeks            = 18
  val defaultDistanceM = 42195
  val defaultVertM     = 0

  // Phase markers (weeksOut → phase). Derived from the published 18-week
  // beginner schedule.
  //   weeks 17 .. 13  base build     (easy only)
  //   weeks 12 .. 8   speed phase    (T = intervals, Th = tempo)
  //   weeks 7 .. 3    strength phase (T = strength reps, Th = MP tempo)
  //   weeks 2 .. 1    sharpen + early taper
  //   week  0         race week

  private def base(w: Int, longMi: Double, weekdayMi: Double): List[TemplateDay] =
    List(
      rest(w, MONDAY),
      easy(w, TUESDAY,   weekdayMi, math.round(weekdayMi * 10).toInt),
      easy(w, WEDNESDAY, weekdayMi, math.round(weekdayMi * 10).toInt),
      easy(w, THURSDAY,  weekdayMi, math.round(weekdayMi * 10).toInt),
      easy(w, FRIDAY,    weekdayMi, math.round(weekdayMi * 10).toInt),
      TemplateDay(w, SATURDAY, WorkoutType.Long,
        targetDistanceM = Some(mi(longMi)),
        targetDurationS = Some(min(math.round(longMi * 10).toInt)),
        intensity = Some("Long & easy")),
      easy(w, SUNDAY, weekdayMi, math.round(weekdayMi * 10).toInt)
    )

  private def speedWeek(w: Int, longMi: Double, intervalsLabel: String): List[TemplateDay] =
    List(
      rest(w, MONDAY),
      TemplateDay(w, TUESDAY, WorkoutType.Intervals,
        targetDurationS = Some(min(60)),
        intensity = Some(intervalsLabel),
        notes = Some("WU 1.5mi, intervals, CD 1.5mi.")),
      easy(w, WEDNESDAY, 5.0, 50),
      TemplateDay(w, THURSDAY, WorkoutType.Tempo,
        targetDistanceM = Some(mi(8)), targetDurationS = Some(min(70)),
        intensity = Some("Tempo: 6mi @ MP+10s/mi"),
        notes = Some("WU 1mi, 6mi tempo, CD 1mi.")),
      easy(w, FRIDAY, 6.0, 60),
      TemplateDay(w, SATURDAY, WorkoutType.Long,
        targetDistanceM = Some(mi(longMi)),
        targetDurationS = Some(min(math.round(longMi * 9).toInt)),
        intensity = Some("Long & easy")),
      easy(w, SUNDAY, 6.0, 60)
    )

  private def strengthWeek(w: Int, longMi: Double, strengthLabel: String): List[TemplateDay] =
    List(
      rest(w, MONDAY),
      TemplateDay(w, TUESDAY, WorkoutType.Intervals,
        targetDurationS = Some(min(70)),
        intensity = Some(strengthLabel),
        notes = Some("Strength reps at 10s/mi faster than MP, long recovery.")),
      easy(w, WEDNESDAY, 7.0, 70),
      TemplateDay(w, THURSDAY, WorkoutType.Tempo,
        targetDistanceM = Some(mi(10)), targetDurationS = Some(min(85)),
        intensity = Some("MP tempo: 8mi @ marathon pace"),
        notes = Some("WU 1mi, 8mi @ MP, CD 1mi.")),
      easy(w, FRIDAY, 6.0, 60),
      TemplateDay(w, SATURDAY, WorkoutType.Long,
        targetDistanceM = Some(mi(longMi)),
        targetDurationS = Some(min(math.round(longMi * 9).toInt)),
        intensity = Some("Long & easy on tired legs")),
      easy(w, SUNDAY, 7.0, 70)
    )

  private val raceWeek: List[TemplateDay] = List(
    rest(0, MONDAY),
    easy(0, TUESDAY,   5.0, 45),
    easy(0, WEDNESDAY, 4.0, 36),
    easy(0, THURSDAY,  3.0, 28),
    rest(0, FRIDAY),
    rest(0, SATURDAY),
    TemplateDay(0, SUNDAY, WorkoutType.Race,
      targetDistanceM = Some(defaultDistanceM),
      intensity = Some("RACE DAY"),
      notes = Some("Trust the cumulative miles. Hold target pace; the back half is where the plan pays off."))
  )

  val template: List[TemplateDay] =
    // Base: weeks 17..13 (5 weeks, gentle build)
    base(17, longMi = 8,  weekdayMi = 4) ++
    base(16, longMi = 9,  weekdayMi = 4) ++
    base(15, longMi = 10, weekdayMi = 5) ++
    base(14, longMi = 10, weekdayMi = 5) ++
    base(13, longMi = 12, weekdayMi = 5) ++
    // Speed: weeks 12..8 (5 weeks)
    speedWeek(12, longMi = 12, "12x400m @ 5k pace, 400m jog") ++
    speedWeek(11, longMi = 13, "8x600m @ 5k pace, 400m jog") ++
    speedWeek(10, longMi = 13, "6x800m @ 5k pace, 400m jog") ++
    speedWeek( 9, longMi = 14, "5x1000m @ 5k pace, 400m jog") ++
    speedWeek( 8, longMi = 14, "4x1200m @ 5k pace, 400m jog") ++
    // Strength: weeks 7..3 (5 weeks)
    strengthWeek(7, longMi = 15, "6x1mi @ 10s faster than MP, 400m jog") ++
    strengthWeek(6, longMi = 15, "4x1.5mi @ 10s faster than MP, 800m jog") ++
    strengthWeek(5, longMi = 16, "3x2mi @ 10s faster than MP, 800m jog") ++
    strengthWeek(4, longMi = 16, "2x3mi @ 10s faster than MP, 1mi jog") ++
    strengthWeek(3, longMi = 16, "5x1mi @ 10s faster than MP, 400m jog") ++
    // Sharpen / early taper
    speedWeek(2, longMi = 16, "5x600m @ 5k pace, 400m jog") ++
    speedWeek(1, longMi = 10, "Easy week — drop intervals to 4x400m + strides") ++
    raceWeek
