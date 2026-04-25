package training.repo

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import training.domain.*

final class AthleteRepo(xa: Transactor[IO]):

  def getActivePlanId(athleteId: AthleteId): IO[Option[PlanId]] =
    sql"select active_plan_id from athlete where id = ${athleteId.value}"
      .query[Option[Long]].unique.transact(xa).map(_.map(PlanId.apply))

  def setActivePlanId(athleteId: AthleteId, planId: PlanId): IO[Int] =
    sql"update athlete set active_plan_id = ${planId.value} where id = ${athleteId.value}"
      .update.run.transact(xa)
