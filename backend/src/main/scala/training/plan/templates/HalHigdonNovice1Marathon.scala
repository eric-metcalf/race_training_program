package training.plan.templates

import training.domain.WorkoutType
import training.plan.PlanTemplate
import training.plan.PlanTemplate.{TemplateDay, easy, mi, min, rest, xtrain}

import java.time.DayOfWeek.*

/** Hal Higdon Novice 1 — Marathon (18 weeks).
  *
  * Approximation of Higdon's classic plan: 4 days of running, 1 cross-train,
  * 2 rest. Long run on Saturday building from 6 → 20 mi with a 12 → 8 → race
  * taper. No speed work; focus is finishing for first-time marathoners.
  *
  * The official plan is at halhigdon.com — workout text is paraphrased here
  * for personal-use within this app.
  */
object HalHigdonNovice1Marathon extends PlanTemplate:

  val key              = "halhigdon-novice1-marathon-18wk"
  val name             = "Hal Higdon Novice 1 — Marathon (18 wk)"
  val description      =
    "The classic 18-week first-marathon plan: 4 runs/week, 1 cross-train, 2 rest. " +
    "Long run climbs from 6 to 20 miles. No speed work. Goal is to cross the finish line."
  val raceCategory     = "Marathon"
  val terrain          = "Road"
  val level            = "Novice"
  val weeks            = 18
  val defaultDistanceM = 42195
  val defaultVertM     = 0

  // Week-by-week long-run distances (Saturday) for weeks 17 → 0:
  //   17 16 15 14 13 12 11 10  9  8  7  6  5  4  3  2  1  0
  //    6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 12  8  RACE
  private val longRunMiles: Map[Int, Double] = Map(
    17 -> 6,  16 -> 7,  15 -> 8,  14 -> 9,
    13 -> 10, 12 -> 11, 11 -> 12, 10 -> 13,
     9 -> 14,  8 -> 15,  7 -> 16,  6 -> 17,
     5 -> 18,  4 -> 19,  3 -> 20,  2 -> 12,  1 -> 8
  )
  // Tuesday/Thursday easy mileage scales gently with the long run.
  private val midweekMiles: Map[Int, Double] = Map(
    17 -> 3,  16 -> 3,  15 -> 3,  14 -> 3,
    13 -> 3,  12 -> 3,  11 -> 4,  10 -> 4,
     9 -> 4,   8 -> 4,   7 -> 4,   6 -> 5,
     5 -> 5,   4 -> 5,   3 -> 5,   2 -> 4,  1 -> 3
  )
  // Wednesday is a slightly longer run for the back half.
  private val wedMiles: Map[Int, Double] = Map(
    17 -> 3,  16 -> 3,  15 -> 4,  14 -> 4,
    13 -> 5,  12 -> 5,  11 -> 5,  10 -> 6,
     9 -> 6,   8 -> 6,   7 -> 7,   6 -> 7,
     5 -> 8,   4 -> 8,   3 -> 8,   2 -> 5,  1 -> 4
  )

  private def buildWeek(w: Int): List[TemplateDay] = List(
    rest(w, MONDAY),
    easy(w, TUESDAY,   midweekMiles(w), math.round(midweekMiles(w) * 10).toInt),
    easy(w, WEDNESDAY, wedMiles(w),     math.round(wedMiles(w)     * 10).toInt),
    easy(w, THURSDAY,  midweekMiles(w), math.round(midweekMiles(w) * 10).toInt),
    rest(w, FRIDAY),
    TemplateDay(w, SATURDAY, WorkoutType.Long,
      targetDistanceM = Some(mi(longRunMiles(w))),
      targetDurationS = Some(min(math.round(longRunMiles(w) * 11).toInt)),
      intensity = Some("Long & easy — conversational pace"),
      notes = Some("Slow it down. Walk through aid stations if you need to.")),
    xtrain(w, SUNDAY, 45)
  )

  // Race week (week 0): drop volume, keep 4 short shakeouts, race Sunday by
  // convention (Higdon's original is Sunday-race).
  private val raceWeek: List[TemplateDay] = List(
    xtrain(0, MONDAY, 30),
    easy(0, TUESDAY,   3.0, 28),
    easy(0, WEDNESDAY, 2.0, 18),
    easy(0, THURSDAY,  2.0, 18),
    rest(0, FRIDAY),
    rest(0, SATURDAY),
    TemplateDay(0, SUNDAY, WorkoutType.Race,
      targetDistanceM = Some(defaultDistanceM),
      intensity = Some("RACE DAY"),
      notes = Some("First marathon. Trust the build. Don't go out too fast."))
  )

  val template: List[TemplateDay] =
    (17 to 1 by -1).toList.flatMap(buildWeek) ++ raceWeek
