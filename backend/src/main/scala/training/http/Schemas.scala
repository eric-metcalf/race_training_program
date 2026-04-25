package training.http

import sttp.tapir.Schema
import training.domain.*

/** Tapir Schema instances for opaque IDs and string-shaped enums. Imported by
  * any module that defines Tapir endpoints involving these types.
  */
object Schemas:

  // Opaque Long-based IDs render as integers in OpenAPI.
  given Schema[AthleteId]        = Schema.schemaForLong.map(l => Some(AthleteId(l)))(_.value)
  given Schema[RaceId]           = Schema.schemaForLong.map(l => Some(RaceId(l)))(_.value)
  given Schema[PlanId]           = Schema.schemaForLong.map(l => Some(PlanId(l)))(_.value)
  given Schema[PlannedWorkoutId] = Schema.schemaForLong.map(l => Some(PlannedWorkoutId(l)))(_.value)
  given Schema[ActivityId]       = Schema.schemaForLong.map(l => Some(ActivityId(l)))(_.value)

  // Enums serialize to strings via our domain codecs.
  given Schema[WorkoutType]    = Schema.schemaForString.map(WorkoutType.fromDb(_).toOption)(WorkoutType.toDb)
  given Schema[MatchStatus]    = Schema.schemaForString.map(MatchStatus.fromDb(_).toOption)(MatchStatus.toDb)
  given Schema[ActivitySource] = Schema.schemaForString.map(ActivitySource.fromDb(_).toOption)(ActivitySource.toDb)
