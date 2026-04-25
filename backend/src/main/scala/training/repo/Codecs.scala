package training.repo

import doobie.Meta
import training.domain.*

/** doobie Meta instances for our enum & opaque-id columns. Imported by repos. */
object Codecs:

  given Meta[WorkoutType] =
    Meta[String].timap(WorkoutType.fromDb(_).fold(sys.error, identity))(WorkoutType.toDb)

  given Meta[MatchStatus] =
    Meta[String].timap(MatchStatus.fromDb(_).fold(sys.error, identity))(MatchStatus.toDb)

  given Meta[ActivitySource] =
    Meta[String].timap(ActivitySource.fromDb(_).fold(sys.error, identity))(ActivitySource.toDb)

  given Meta[AthleteId]        = Meta[Long].imap(AthleteId.apply)(_.value)
  given Meta[RaceId]           = Meta[Long].imap(RaceId.apply)(_.value)
  given Meta[PlanId]           = Meta[Long].imap(PlanId.apply)(_.value)
  given Meta[PlannedWorkoutId] = Meta[Long].imap(PlannedWorkoutId.apply)(_.value)
  given Meta[ActivityId]       = Meta[Long].imap(ActivityId.apply)(_.value)
