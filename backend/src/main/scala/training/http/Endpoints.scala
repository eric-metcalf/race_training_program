package training.http

import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import training.domain.*
import training.http.Schemas.given

import java.time.LocalDate

object Endpoints:

  // -- response views (JSON shapes) -----------------------------------------

  final case class PlannedWorkoutView(
      id: Long,
      date: LocalDate,
      `type`: String,
      targetDistanceM: Option[Int],
      targetDurationS: Option[Int],
      targetVertM: Option[Int],
      intensity: Option[String],
      notes: Option[String],
      matchStatus: String,
      matchedActivityId: Option[Long]
  ) derives Encoder.AsObject, Decoder

  final case class ActivityView(
      id: Long,
      source: String,
      externalId: String,
      startedAt: String,
      distanceM: Int,
      movingSeconds: Int,
      elevationGainM: Option[Int],
      avgHr: Option[Int],
      activityType: String,
      name: Option[String]
  ) derives Encoder.AsObject, Decoder

  final case class RegenerateResponse(planId: Long, workoutsInserted: Int)
      derives Encoder.AsObject, Decoder

  final case class WorkoutDetailView(
      planned: PlannedWorkoutView,
      matchedActivity: Option[ActivityView]
  ) derives Encoder.AsObject, Decoder

  final case class WorkoutEdit(
      `type`: String,
      targetDistanceM: Option[Int],
      targetDurationS: Option[Int],
      targetVertM: Option[Int],
      intensity: Option[String],
      notes: Option[String]
  ) derives Encoder.AsObject, Decoder

  final case class TemplateSummary(
      key: String,
      name: String,
      description: String,
      raceCategory: String,
      terrain: String,
      level: String,
      weeks: Int,
      defaultDistanceM: Int,
      defaultVertM: Int
  ) derives Encoder.AsObject, Decoder

  final case class TemplateWorkoutView(
      weeksOut: Int,
      dayOfWeek: Int,        // 1 = Mon .. 7 = Sun
      `type`: String,
      targetDistanceM: Option[Int],
      targetDurationS: Option[Int],
      targetVertM: Option[Int],
      intensity: Option[String],
      notes: Option[String]
  ) derives Encoder.AsObject, Decoder

  final case class TemplateDetailView(
      summary: TemplateSummary,
      workouts: List[TemplateWorkoutView]
  ) derives Encoder.AsObject, Decoder

  final case class CreatePlanRequest(
      templateKey: String,
      raceName: String,
      raceDate: LocalDate,
      location: Option[String],
      distanceM: Option[Int],
      vertM: Option[Int],
      notes: Option[String]
  ) derives Encoder.AsObject, Decoder

  final case class CreatePlanResponse(
      planId: Long,
      raceId: Long,
      workoutsInserted: Int
  ) derives Encoder.AsObject, Decoder

  /** Activity summary uploaded from a parsed .FIT file. */
  final case class CreateActivityRequest(
      source: String,                 // "fit" or "manual"
      externalId: String,             // sha1 of source bytes when source=fit
      startedAt: String,              // ISO-8601 instant string (always UTC)
      localDate: Option[LocalDate],   // the date the user perceives the run
                                      // happened on, in their TZ. Sent by the
                                      // browser via Intl. Backend falls back
                                      // to UTC-derived date if omitted.
      distanceM: Int,
      movingSeconds: Int,
      elevationGainM: Option[Int],
      avgHr: Option[Int],
      activityType: String,           // "run", "trail_run", "ride", etc.
      name: Option[String]
  ) derives Encoder.AsObject, Decoder

  final case class CreateActivityResponse(
      activityId: Long,
      duplicate: Boolean,             // true if external_id already existed
      matchedPlannedWorkoutId: Option[Long],
      matchStatus: Option[String]
  ) derives Encoder.AsObject, Decoder

  /** Re-attach an activity to a different day's planned workout. Used to fix
    * up activities that landed on the wrong day (e.g., evening runs that got
    * matched to the next UTC day before the localDate fix). */
  final case class RematchRequest(localDate: LocalDate) derives Encoder.AsObject, Decoder

  final case class RematchResponse(
      activityId: Long,
      previousPlannedWorkoutId: Option[Long],
      matchedPlannedWorkoutId: Option[Long],
      matchStatus: Option[String]
  ) derives Encoder.AsObject, Decoder

  /** A training plan with the race it's for, for the /plans list view. */
  final case class PlanSummaryView(
      planId: Long,
      raceId: Long,
      templateKey: String,
      templateName: Option[String],    // from TemplateCatalog; None if template was removed
      raceName: String,
      raceDate: LocalDate,
      distanceM: Int,
      vertM: Int,
      location: Option[String],
      workoutCount: Int,
      generatedAt: String,             // ISO instant
      isActive: Boolean
  ) derives Encoder.AsObject, Decoder

  final case class OkResponse(ok: Boolean) derives Encoder.AsObject, Decoder

  final case class ApiError(message: String) derives Encoder.AsObject, Decoder

  private val base = endpoint
    .in("api")
    .errorOut(jsonBody[ApiError].and(statusCode(sttp.model.StatusCode.InternalServerError)))

  // -- existing endpoints (now resolve via active plan) ---------------------

  val getCurrentRace =
    base.get
      .in("race" / "current")
      .out(jsonBody[Race])
      .errorOutVariant(
        oneOfVariant(statusCode(sttp.model.StatusCode.NotFound).and(jsonBody[ApiError]))
      )
      .description("Returns the race for the athlete's active plan, or 404 if none is set")

  val getPlan =
    base.get
      .in("plan")
      .in(query[LocalDate]("from"))
      .in(query[LocalDate]("to"))
      .out(jsonBody[List[PlannedWorkoutView]])

  val getWorkoutByDate =
    base.get
      .in("plan" / path[LocalDate]("date"))
      .out(jsonBody[WorkoutDetailView])
      .errorOutVariant(
        oneOfVariant(statusCode(sttp.model.StatusCode.NotFound).and(jsonBody[ApiError]))
      )

  val regeneratePlan =
    base.post
      .in("plan" / "regenerate")
      .out(jsonBody[RegenerateResponse])
      .description("Idempotently regenerate the active plan from its template — overwrites edits")

  val updateWorkout =
    base.put
      .in("plan" / path[Long]("id"))
      .in(jsonBody[WorkoutEdit])
      .out(jsonBody[PlannedWorkoutView])
      .errorOutVariant(
        oneOfVariant(statusCode(sttp.model.StatusCode.NotFound).and(jsonBody[ApiError]))
      )

  val listActivities =
    base.get
      .in("activities")
      .in(query[Option[Int]]("limit"))
      .out(jsonBody[List[ActivityView]])

  // -- new templates + plans endpoints --------------------------------------

  val listTemplates =
    base.get
      .in("templates")
      .out(jsonBody[List[TemplateSummary]])
      .description("All available training plan templates")

  val getTemplate =
    base.get
      .in("templates" / path[String]("key"))
      .out(jsonBody[TemplateDetailView])
      .errorOutVariant(
        oneOfVariant(statusCode(sttp.model.StatusCode.NotFound).and(jsonBody[ApiError]))
      )
      .description("A template's metadata + workout-by-day preview (no dates)")

  val createPlan =
    base.post
      .in("plans")
      .in(jsonBody[CreatePlanRequest])
      .out(jsonBody[CreatePlanResponse])
      .description("Create a race + training_plan from a template, expand workouts, set active")

  val createActivity =
    base.post
      .in("activities")
      .in(jsonBody[CreateActivityRequest])
      .out(jsonBody[CreateActivityResponse])
      .description("Upsert an activity (e.g. parsed from a .FIT file) and re-match its date's planned workout")

  val rematchActivity =
    base.post
      .in("activities" / path[Long]("id") / "rematch")
      .in(jsonBody[RematchRequest])
      .out(jsonBody[RematchResponse])
      .errorOutVariant(
        oneOfVariant(statusCode(sttp.model.StatusCode.NotFound).and(jsonBody[ApiError]))
      )
      .description("Move an existing activity's match to a different day's planned workout")

  val listPlans =
    base.get
      .in("plans")
      .out(jsonBody[List[PlanSummaryView]])
      .description("All training plans for the athlete, with race details and active flag")

  val activatePlan =
    base.post
      .in("plans" / path[Long]("id") / "activate")
      .out(jsonBody[OkResponse])
      .errorOutVariant(
        oneOfVariant(statusCode(sttp.model.StatusCode.NotFound).and(jsonBody[ApiError]))
      )
      .description("Set this plan as the athlete's active plan")

  val deletePlan =
    base.delete
      .in("plans" / path[Long]("id"))
      .out(jsonBody[OkResponse])
      .errorOutVariant(
        oneOfVariant(statusCode(sttp.model.StatusCode.NotFound).and(jsonBody[ApiError]))
      )
      .description("Delete a plan (and its race, cascading to workouts + matches)")

  val all = List(
    getCurrentRace,
    getPlan,
    getWorkoutByDate,
    regeneratePlan,
    updateWorkout,
    listActivities,
    createActivity,
    rematchActivity,
    listTemplates,
    getTemplate,
    createPlan,
    listPlans,
    activatePlan,
    deletePlan
  )
