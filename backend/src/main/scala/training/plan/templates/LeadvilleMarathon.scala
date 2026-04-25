package training.plan.templates

import training.domain.WorkoutType
import training.plan.PlanTemplate
import training.plan.PlanTemplate.{TemplateDay, easy, ft, mi, min, rest, xtrain}

import java.time.DayOfWeek.*

/** Leadville Trail Marathon — 9-week peak/taper template tuned for a moderate
  * base (20–40 mpw, mostly road). Vert-over-volume; weekend back-to-backs to
  * mimic accumulated alpine fatigue; 10-day taper.
  */
object LeadvilleMarathon extends PlanTemplate:

  val key             = "leadville-marathon-9wk-moderate"
  val name            = "Leadville Trail Marathon — 9 wk (moderate base)"
  val description     =
    "Peak/taper block for the Leadville Trail Marathon. Designed for a moderate-base " +
    "runner targeting a finish, not a time. Emphasizes vertical gain over volume, " +
    "weekly hill repeats, weekend back-to-backs, and a 10-day taper. Topology assumes " +
    "you'll power-hike Mosquito Pass."
  val raceCategory    = "Trail Marathon"
  val terrain         = "Mountain"
  val level           = "Intermediate"
  val weeks           = 9
  val defaultDistanceM = 42500
  val defaultVertM     = 1830

  val template: List[TemplateDay] = List(

    // -- Week 8 (build start) ------------------------------------------------
    rest(8, MONDAY),
    TemplateDay(8, TUESDAY, WorkoutType.Vert,
      targetDurationS = Some(min(45)), targetVertM = Some(ft(800)),
      intensity = Some("Hills: 6x90s hard up, easy down"),
      notes = Some("Find ~6% grade. Steady effort, not all-out.")),
    easy(8, WEDNESDAY, 4.0, 38),
    TemplateDay(8, THURSDAY, WorkoutType.Tempo,
      targetDistanceM = Some(mi(5)), targetDurationS = Some(min(45)),
      intensity = Some("4mi @ marathon-pace effort"),
      notes = Some("1mi WU, 4mi @ MP, easy CD.")),
    xtrain(8, FRIDAY, 45),
    TemplateDay(8, SATURDAY, WorkoutType.Long,
      targetDistanceM = Some(mi(10)), targetDurationS = Some(min(110)),
      targetVertM = Some(ft(2000)),
      intensity = Some("Z2 with hilly route"),
      notes = Some("Run a hilly route. Power-hike anything > 12% grade.")),
    easy(8, SUNDAY, 4.0, 40),

    // -- Week 7 --------------------------------------------------------------
    rest(7, MONDAY),
    TemplateDay(7, TUESDAY, WorkoutType.Vert,
      targetDurationS = Some(min(50)), targetVertM = Some(ft(1000)),
      intensity = Some("Hills: 6x2min hard up, easy down")),
    easy(7, WEDNESDAY, 5.0, 48),
    TemplateDay(7, THURSDAY, WorkoutType.Tempo,
      targetDistanceM = Some(mi(6)), targetDurationS = Some(min(54)),
      intensity = Some("4mi @ MP w/ rolling hills")),
    xtrain(7, FRIDAY, 45),
    TemplateDay(7, SATURDAY, WorkoutType.Long,
      targetDistanceM = Some(mi(12)), targetDurationS = Some(min(140)),
      targetVertM = Some(ft(2500)),
      notes = Some("Long w/ vert. Practice nutrition: gel every 30 min.")),
    easy(7, SUNDAY, 5.0, 50),

    // -- Week 6 --------------------------------------------------------------
    rest(6, MONDAY),
    TemplateDay(6, TUESDAY, WorkoutType.Intervals,
      targetDurationS = Some(min(50)),
      intensity = Some("5x800m @ 5k effort, 400m jog rest")),
    easy(6, WEDNESDAY, 5.0, 50),
    TemplateDay(6, THURSDAY, WorkoutType.Vert,
      targetDurationS = Some(min(60)), targetVertM = Some(ft(1500)),
      intensity = Some("Hill repeats: 8x90s steep, hike down")),
    xtrain(6, FRIDAY, 60),
    TemplateDay(6, SATURDAY, WorkoutType.Long,
      targetDistanceM = Some(mi(13)), targetDurationS = Some(min(160)),
      targetVertM = Some(ft(3000)),
      notes = Some("First 'big' day. Power-hike steep climbs.")),
    TemplateDay(6, SUNDAY, WorkoutType.Vert,
      targetDistanceM = Some(mi(6)), targetDurationS = Some(min(80)),
      targetVertM = Some(ft(1200)),
      intensity = Some("Easy effort on tired legs"),
      notes = Some("Back-to-back day. Run by feel; walk breaks fine.")),

    // -- Week 5 (cutback) ----------------------------------------------------
    rest(5, MONDAY),
    TemplateDay(5, TUESDAY, WorkoutType.Vert,
      targetDurationS = Some(min(45)), targetVertM = Some(ft(900))),
    easy(5, WEDNESDAY, 4.0, 40),
    TemplateDay(5, THURSDAY, WorkoutType.Tempo,
      targetDistanceM = Some(mi(5)), targetDurationS = Some(min(45)),
      intensity = Some("3mi @ half-marathon pace")),
    xtrain(5, FRIDAY, 45),
    TemplateDay(5, SATURDAY, WorkoutType.Long,
      targetDistanceM = Some(mi(10)), targetDurationS = Some(min(115)),
      targetVertM = Some(ft(2000)),
      notes = Some("Cutback long. Recovery week.")),
    easy(5, SUNDAY, 5.0, 50),

    // -- Week 4 --------------------------------------------------------------
    rest(4, MONDAY),
    TemplateDay(4, TUESDAY, WorkoutType.Intervals,
      targetDurationS = Some(min(55)),
      intensity = Some("6x1km @ 10k effort, 90s rest")),
    easy(4, WEDNESDAY, 6.0, 60),
    TemplateDay(4, THURSDAY, WorkoutType.Vert,
      targetDurationS = Some(min(65)), targetVertM = Some(ft(1800)),
      intensity = Some("10x90s hill repeats steep grade")),
    xtrain(4, FRIDAY, 60),
    TemplateDay(4, SATURDAY, WorkoutType.Long,
      targetDistanceM = Some(mi(15)), targetDurationS = Some(min(190)),
      targetVertM = Some(ft(3500)),
      notes = Some("Practice hike-run cadence on the steep stuff.")),
    TemplateDay(4, SUNDAY, WorkoutType.Vert,
      targetDistanceM = Some(mi(7)), targetDurationS = Some(min(100)),
      targetVertM = Some(ft(1500)),
      intensity = Some("Easy on tired legs"),
      notes = Some("Back-to-back. Should feel like the second half of Leadville.")),

    // -- Week 3 --------------------------------------------------------------
    rest(3, MONDAY),
    TemplateDay(3, TUESDAY, WorkoutType.Vert,
      targetDurationS = Some(min(60)), targetVertM = Some(ft(1500)),
      intensity = Some("Hill repeats")),
    easy(3, WEDNESDAY, 5.0, 50),
    TemplateDay(3, THURSDAY, WorkoutType.Tempo,
      targetDistanceM = Some(mi(7)), targetDurationS = Some(min(63)),
      intensity = Some("5mi @ MP")),
    xtrain(3, FRIDAY, 60),
    TemplateDay(3, SATURDAY, WorkoutType.Long,
      targetDistanceM = Some(mi(16)), targetDurationS = Some(min(210)),
      targetVertM = Some(ft(4000)),
      notes = Some("Longest pre-peak. Dial in race-day fueling and shoes.")),
    TemplateDay(3, SUNDAY, WorkoutType.Vert,
      targetDistanceM = Some(mi(8)), targetDurationS = Some(min(110)),
      targetVertM = Some(ft(1500))),

    // -- Week 2 (PEAK) -------------------------------------------------------
    rest(2, MONDAY),
    TemplateDay(2, TUESDAY, WorkoutType.Vert,
      targetDurationS = Some(min(60)), targetVertM = Some(ft(1500)),
      intensity = Some("Hills: 8x2min")),
    easy(2, WEDNESDAY, 5.0, 50),
    TemplateDay(2, THURSDAY, WorkoutType.Tempo,
      targetDistanceM = Some(mi(6)), targetDurationS = Some(min(55)),
      intensity = Some("Race-pace 4mi"),
      notes = Some("Last hard tempo. Should feel controlled, not maxed.")),
    xtrain(2, FRIDAY, 45),
    TemplateDay(2, SATURDAY, WorkoutType.Long,
      targetDistanceM = Some(mi(18)), targetDurationS = Some(min(245)),
      targetVertM = Some(ft(4500)),
      intensity = Some("Race-sim pace"),
      notes = Some("Peak long run. Run the steepest, most race-like trail you can find.")),
    TemplateDay(2, SUNDAY, WorkoutType.Vert,
      targetDistanceM = Some(mi(8)), targetDurationS = Some(min(110)),
      targetVertM = Some(ft(1800)),
      intensity = Some("Tired-legs Z2"),
      notes = Some("Final back-to-back of the build.")),

    // -- Week 1 (taper down) -------------------------------------------------
    rest(1, MONDAY),
    TemplateDay(1, TUESDAY, WorkoutType.Vert,
      targetDurationS = Some(min(45)), targetVertM = Some(ft(800)),
      intensity = Some("Short hills: 5x90s")),
    easy(1, WEDNESDAY, 5.0, 50),
    TemplateDay(1, THURSDAY, WorkoutType.Intervals,
      targetDurationS = Some(min(40)),
      intensity = Some("4x400m @ 5k effort + 6x100m strides")),
    xtrain(1, FRIDAY, 30),
    TemplateDay(1, SATURDAY, WorkoutType.Long,
      targetDistanceM = Some(mi(12)), targetDurationS = Some(min(140)),
      targetVertM = Some(ft(2500)),
      notes = Some("Mid-long shakedown. Race nutrition, race shoes.")),
    easy(1, SUNDAY, 5.0, 50),

    // -- Week 0 (race week) --------------------------------------------------
    rest(0, MONDAY),
    TemplateDay(0, TUESDAY, WorkoutType.Easy,
      targetDistanceM = Some(mi(5)), targetDurationS = Some(min(45)),
      intensity = Some("Easy + 4 strides"),
      notes = Some("Travel to Leadville this week if you can — altitude acclimation.")),
    easy(0, WEDNESDAY, 4.0, 40),
    TemplateDay(0, THURSDAY, WorkoutType.Easy,
      targetDistanceM = Some(mi(3)), targetDurationS = Some(min(28)),
      intensity = Some("Shakeout + 4x100m strides")),
    rest(0, FRIDAY),
    TemplateDay(0, SATURDAY, WorkoutType.Race,
      targetDistanceM = Some(42500), targetVertM = Some(1830),
      intensity = Some("RACE DAY"),
      notes = Some("Leadville Trail Marathon. Hike the steep stuff. Eat early, eat often.")),
    TemplateDay(0, SUNDAY, WorkoutType.Recovery,
      targetDurationS = Some(min(30)),
      notes = Some("Easy walk only. Celebrate."))
  )
