package training.repo

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import training.domain.*
import training.repo.Codecs.given

import java.time.LocalDate

final class RaceRepo(xa: Transactor[IO]):

  def find(raceId: RaceId): IO[Option[Race]] =
    sql"""
      select id, name, race_date, distance_m, vert_m, location, notes
      from race where id = ${raceId.value}
    """.query[Race].option.transact(xa)

  def insert(
      name: String,
      raceDate: LocalDate,
      distanceM: Int,
      vertM: Int,
      location: Option[String],
      notes: Option[String]
  ): IO[RaceId] =
    sql"""
      insert into race (name, race_date, distance_m, vert_m, location, notes)
      values ($name, $raceDate, $distanceM, $vertM, $location, $notes)
    """.update.withUniqueGeneratedKeys[Long]("id").map(RaceId.apply).transact(xa)

  /** Delete the race. training_plan → planned_workout → workout_match all
    * cascade. Row count returned (0 if not found).
    */
  def delete(raceId: RaceId): IO[Int] =
    sql"delete from race where id = ${raceId.value}".update.run.transact(xa)
