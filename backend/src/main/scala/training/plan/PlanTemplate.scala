package training.plan

import training.domain.WorkoutType

import java.time.DayOfWeek

/** A reusable training plan template. Instances live in `training.plan.templates`
  * and are listed in `TemplateCatalog`. They emit a list of `TemplateDay`s that
  * the `Generator` expands against a chosen race date.
  *
  * weeksOut convention: 0 = race week (the calendar week containing race day),
  * higher = earlier in the build.
  */
trait PlanTemplate:
  /** Stable, URL-safe identifier (saved on training_plan.template_key). */
  def key: String
  /** Human-readable name shown in the catalog. */
  def name: String
  /** A short paragraph explaining who it's for and the weekly shape. */
  def description: String
  /** "Marathon", "Half Marathon", "Trail Marathon", "100K", etc. */
  def raceCategory: String
  /** "Road", "Trail", "Mountain". */
  def terrain: String
  /** "Novice", "Intermediate", "Advanced". */
  def level: String
  /** Total weeks of training in the template (race week counted as 1). */
  def weeks: Int
  /** Default total race distance in meters (used to seed the race row). */
  def defaultDistanceM: Int
  /** Default total elevation gain in meters (0 for road plans). */
  def defaultVertM: Int
  /** The day-by-day workout list. weeksOut = 0 is race week. */
  def template: List[PlanTemplate.TemplateDay]

object PlanTemplate:
  final case class TemplateDay(
      weeksOut: Int,
      dayOfWeek: DayOfWeek,
      workoutType: WorkoutType,
      targetDistanceM: Option[Int]   = None,
      targetDurationS: Option[Int]   = None,
      targetVertM: Option[Int]       = None,
      intensity: Option[String]      = None,
      notes: Option[String]          = None
  )

  // -- shared constructors used by template files ---------------------------

  inline def mi(n: Double): Int  = math.round(n * 1609.34).toInt
  inline def ft(n: Int): Int     = math.round(n * 0.3048).toInt
  inline def min(n: Int): Int    = n * 60

  def rest(w: Int, d: DayOfWeek): TemplateDay =
    TemplateDay(w, d, WorkoutType.Rest, notes = Some("Rest day."))

  def xtrain(w: Int, d: DayOfWeek, mins: Int): TemplateDay =
    TemplateDay(w, d, WorkoutType.XTrain, targetDurationS = Some(min(mins)),
      notes = Some("Bike, swim, or strength. Low impact."))

  def easy(w: Int, d: DayOfWeek, miles: Double, mins: Int): TemplateDay =
    TemplateDay(w, d, WorkoutType.Easy, targetDistanceM = Some(mi(miles)),
      targetDurationS = Some(min(mins)), intensity = Some("Z2 / conversational"))
