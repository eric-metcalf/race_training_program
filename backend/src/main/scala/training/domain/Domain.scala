package training.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*

import java.time.{Instant, LocalDate}

opaque type AthleteId = Long
object AthleteId:
  def apply(v: Long): AthleteId            = v
  extension (a: AthleteId) def value: Long = a
  given Encoder[AthleteId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[AthleteId] = Decoder.decodeLong.map(AthleteId.apply)

opaque type RaceId = Long
object RaceId:
  def apply(v: Long): RaceId            = v
  extension (a: RaceId) def value: Long = a
  given Encoder[RaceId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[RaceId] = Decoder.decodeLong.map(RaceId.apply)

opaque type PlanId = Long
object PlanId:
  def apply(v: Long): PlanId            = v
  extension (a: PlanId) def value: Long = a
  given Encoder[PlanId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[PlanId] = Decoder.decodeLong.map(PlanId.apply)

opaque type PlannedWorkoutId = Long
object PlannedWorkoutId:
  def apply(v: Long): PlannedWorkoutId            = v
  extension (a: PlannedWorkoutId) def value: Long = a
  given Encoder[PlannedWorkoutId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[PlannedWorkoutId] = Decoder.decodeLong.map(PlannedWorkoutId.apply)

opaque type ActivityId = Long
object ActivityId:
  def apply(v: Long): ActivityId            = v
  extension (a: ActivityId) def value: Long = a
  given Encoder[ActivityId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[ActivityId] = Decoder.decodeLong.map(ActivityId.apply)

enum WorkoutType derives CanEqual:
  case Easy, Long, Vert, Tempo, Intervals, Recovery, Race, XTrain, Rest

object WorkoutType:
  def fromDb(s: String): Either[String, WorkoutType] = s match
    case "easy"      => Right(Easy)
    case "long"      => Right(Long)
    case "vert"      => Right(Vert)
    case "tempo"     => Right(Tempo)
    case "intervals" => Right(Intervals)
    case "recovery"  => Right(Recovery)
    case "race"      => Right(Race)
    case "xtrain"    => Right(XTrain)
    case "rest"      => Right(Rest)
    case other       => Left(s"unknown workout_type: $other")

  def toDb(w: WorkoutType): String = w match
    case Easy      => "easy"
    case Long      => "long"
    case Vert      => "vert"
    case Tempo     => "tempo"
    case Intervals => "intervals"
    case Recovery  => "recovery"
    case Race      => "race"
    case XTrain    => "xtrain"
    case Rest      => "rest"

  given Encoder[WorkoutType] = Encoder.encodeString.contramap(toDb)
  given Decoder[WorkoutType] = Decoder.decodeString.emap(fromDb)

enum MatchStatus derives CanEqual:
  case Completed, Partial, Modified, Missed, Pending

object MatchStatus:
  def fromDb(s: String): Either[String, MatchStatus] = s match
    case "completed" => Right(Completed)
    case "partial"   => Right(Partial)
    case "modified"  => Right(Modified)
    case "missed"    => Right(Missed)
    case "pending"   => Right(Pending)
    case other       => Left(s"unknown match status: $other")

  def toDb(m: MatchStatus): String = m match
    case Completed => "completed"
    case Partial   => "partial"
    case Modified  => "modified"
    case Missed    => "missed"
    case Pending   => "pending"

  given Encoder[MatchStatus] = Encoder.encodeString.contramap(toDb)
  given Decoder[MatchStatus] = Decoder.decodeString.emap(fromDb)

enum ActivitySource derives CanEqual:
  case Strava, Fit

object ActivitySource:
  def fromDb(s: String): Either[String, ActivitySource] = s match
    case "strava" => Right(Strava)
    case "fit"    => Right(Fit)
    case other    => Left(s"unknown source: $other")

  def toDb(s: ActivitySource): String = s match
    case Strava => "strava"
    case Fit    => "fit"

  given Encoder[ActivitySource] = Encoder.encodeString.contramap(toDb)
  given Decoder[ActivitySource] = Decoder.decodeString.emap(fromDb)

final case class Race(
    id: RaceId,
    name: String,
    raceDate: LocalDate,
    distanceM: Int,
    vertM: Int,
    location: Option[String],
    notes: Option[String]
) derives Encoder.AsObject, Decoder

final case class TrainingPlan(
    id: PlanId,
    raceId: RaceId,
    athleteId: AthleteId,
    templateKey: String,
    generatedAt: Instant
) derives Encoder.AsObject, Decoder

final case class PlannedWorkout(
    id: PlannedWorkoutId,
    planId: PlanId,
    date: LocalDate,
    workoutType: WorkoutType,
    targetDistanceM: Option[Int],
    targetDurationS: Option[Int],
    targetVertM: Option[Int],
    intensity: Option[String],
    notes: Option[String]
) derives Encoder.AsObject, Decoder

final case class Activity(
    id: ActivityId,
    athleteId: AthleteId,
    source: ActivitySource,
    externalId: String,
    startedAt: Instant,
    distanceM: Int,
    movingSeconds: Int,
    elevationGainM: Option[Int],
    avgHr: Option[Int],
    activityType: String,
    name: Option[String]
) derives Encoder.AsObject, Decoder

final case class WorkoutMatch(
    plannedWorkoutId: PlannedWorkoutId,
    activityId: Option[ActivityId],
    status: MatchStatus,
    computedAt: Instant
) derives Encoder.AsObject, Decoder
