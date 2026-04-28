package training.http

import cats.effect.IO
import cats.syntax.all.*
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.server.ServerEndpoint
import training.config.AppConfig
import training.domain.*
import training.http.Endpoints.*
import training.plan.{Generator, Matcher, PlanTemplate, TemplateCatalog}
import training.repo.{ActivityRepo, AthleteRepo, MatchRepo, PlanRepo, RaceRepo}
import training.repo.ActivityRepo.ActivityInsert

import java.time.{Instant, LocalDate, ZoneOffset}
import java.time.format.DateTimeFormatter

final class Routes(
    cfg: AppConfig,
    raceRepo: RaceRepo,
    planRepo: PlanRepo,
    activityRepo: ActivityRepo,
    matchRepo: MatchRepo,
    athleteRepo: AthleteRepo
):
  private val athleteId = AthleteId(cfg.athleteId)

  // -- helpers --------------------------------------------------------------

  private val noActivePlan: ApiError =
    ApiError("no active plan — pick a template at /templates")

  /** Resolve the active plan + race + template for the current athlete. */
  private def withActivePlan[A](
      f: (TrainingPlan, Race, PlanTemplate) => IO[Either[ApiError, A]]
  ): IO[Either[ApiError, A]] =
    athleteRepo.getActivePlanId(athleteId).flatMap {
      case None         => IO.pure(Left(noActivePlan))
      case Some(planId) =>
        planRepo.findPlanById(planId).flatMap {
          case None       => IO.pure(Left(noActivePlan))
          case Some(plan) =>
            raceRepo.find(plan.raceId).flatMap {
              case None       => IO.pure(Left(ApiError(s"race ${plan.raceId.value} missing")))
              case Some(race) =>
                TemplateCatalog.find(plan.templateKey) match
                  case Some(t) => f(plan, race, t)
                  case None    => IO.pure(Left(ApiError(s"unknown template_key: ${plan.templateKey}")))
            }
        }
    }

  // -- handlers -------------------------------------------------------------

  private val getCurrentRaceLogic = getCurrentRace.serverLogic { _ =>
    withActivePlan((_, race, _) => IO.pure(Right(race)))
  }

  private val getPlanLogic = getPlan.serverLogic { case (from, to) =>
    athleteRepo.getActivePlanId(athleteId).flatMap {
      case None         => IO.pure(Right(List.empty[PlannedWorkoutView]))
      case Some(planId) =>
        for
          rows    <- planRepo.listInRange(planId, from, to)
          matches <- rows.traverse(p => matchRepo.find(p.id).map(p -> _))
        yield Right(matches.map((p, m) => toView(p, m)))
    }
  }

  private val regeneratePlanLogic = regeneratePlan.serverLogic { _ =>
    withActivePlan { (plan, race, tmpl) =>
      Generator
        .regenerate(planRepo, plan.id, race.raceDate, tmpl.template)
        .map(n => Right(RegenerateResponse(plan.id.value, n)))
    }
  }

  private val getWorkoutByDateLogic = getWorkoutByDate.serverLogic { date =>
    athleteRepo.getActivePlanId(athleteId).flatMap {
      case None => IO.pure(Left(noActivePlan))
      case Some(planId) =>
        planRepo.findByDate(planId, date).flatMap {
          case None    => IO.pure(Left(ApiError(s"no planned workout on $date")))
          case Some(p) =>
            for
              m       <- matchRepo.find(p.id)
              actOpt  <- m.flatMap(_.activityId) match
                           case Some(aid) =>
                             activityRepo.list(500).map(_.find(_.id == aid))
                           case None => IO.pure(None)
            yield Right(WorkoutDetailView(toView(p, m), actOpt.map(toActivityView)))
        }
    }
  }

  private val updateWorkoutLogic = updateWorkout.serverLogic { case (id, edit) =>
    val pwId = PlannedWorkoutId(id)
    WorkoutType.fromDb(edit.`type`) match
      case Left(err) => IO.pure(Left(ApiError(err)))
      case Right(wt) =>
        for
          n     <- planRepo.updateWorkout(
                     pwId, wt,
                     edit.targetDistanceM, edit.targetDurationS, edit.targetVertM,
                     edit.intensity, edit.notes
                   )
          fresh <- if n == 0 then IO.pure(None) else planRepo.findById(pwId)
          m     <- fresh.fold(IO.pure(None))(p => matchRepo.find(p.id))
        yield fresh match
          case Some(p) => Right(toView(p, m))
          case None    => Left(ApiError(s"no planned workout with id $id"))
    }

  private val listActivitiesLogic = listActivities.serverLogic { limit =>
    activityRepo.list(limit.getOrElse(50)).map(xs => Right(xs.map(toActivityView)))
  }

  private val rematchActivityLogic = rematchActivity.serverLogic { case (id, req) =>
    val aid = ActivityId(id)
    activityRepo.findById(aid).flatMap {
      case None => IO.pure(Left(ApiError(s"no activity with id $id")))
      case Some(_) =>
        for
          previous <- matchRepo.findByActivity(aid)
          _        <- matchRepo.detachActivity(aid)
          today    =  LocalDate.now()
          info     <- matchOnActiveDay(req.localDate, today)
        yield Right(RematchResponse(
          activityId               = id,
          previousPlannedWorkoutId = previous.map(_.value),
          matchedPlannedWorkoutId  = info.map(_._1.value),
          matchStatus              = info.map((_, s) => MatchStatus.toDb(s))
        ))
    }
  }

  private val createActivityLogic = createActivity.serverLogic { req =>
    ActivitySource.fromDb(req.source) match
      case Left(err)     => IO.pure(Left(ApiError(err)))
      case Right(source) =>
        val started =
          try Instant.parse(req.startedAt)
          catch case _: Throwable => Instant.EPOCH
        if started == Instant.EPOCH then
          IO.pure(Left(ApiError(s"invalid startedAt: ${req.startedAt}")))
        else
          val insert = ActivityInsert(
            athleteId      = athleteId,
            source         = source,
            externalId     = req.externalId,
            startedAt      = started,
            distanceM      = req.distanceM,
            movingSeconds  = req.movingSeconds,
            elevationGainM = req.elevationGainM,
            avgHr          = req.avgHr,
            activityType   = req.activityType,
            name           = req.name,
            rawJson        = "{}"
          )
          for
            (actId, wasNew) <- activityRepo.upsert(insert)
            today           =  LocalDate.now()
            // Prefer the client-provided local date (browser knows the user's
            // TZ); fall back to UTC-derived if the client didn't send it.
            // FIT files store start_time in UTC, so the naive UTC-derived
            // date is wrong for evening workouts in negative-offset TZs
            // (e.g., a 6pm Mountain Time run becomes "tomorrow" in UTC).
            activityDate    =  req.localDate
                                 .getOrElse(started.atOffset(ZoneOffset.UTC).toLocalDate)
            matchInfo       <- matchOnActiveDay(activityDate, today)
          yield Right(CreateActivityResponse(
            activityId              = actId.value,
            duplicate               = !wasNew,
            matchedPlannedWorkoutId = matchInfo.map(_._1.value),
            matchStatus             = matchInfo.map((_, s) => MatchStatus.toDb(s))
          ))
  }

  private def matchOnActiveDay(
      date: LocalDate,
      today: LocalDate
  ): IO[Option[(PlannedWorkoutId, MatchStatus)]] =
    athleteRepo.getActivePlanId(athleteId).flatMap {
      case None         => IO.pure(None)
      case Some(planId) =>
        planRepo.findByDate(planId, date).flatMap {
          case None    => IO.pure(None)
          case Some(p) =>
            Matcher
              .matchOne(activityRepo, matchRepo, athleteId, p, today)
              .map(s => Some(p.id -> s))
        }
    }

  // -- templates + plans ----------------------------------------------------

  private val listTemplatesLogic = listTemplates.serverLogic { _ =>
    IO.pure(Right(TemplateCatalog.all.map(toSummary)))
  }

  private val getTemplateLogic = getTemplate.serverLogic { key =>
    TemplateCatalog.find(key) match
      case None => IO.pure(Left(ApiError(s"unknown template: $key")))
      case Some(t) =>
        IO.pure(Right(TemplateDetailView(
          summary  = toSummary(t),
          workouts = t.template.map(d => TemplateWorkoutView(
            weeksOut         = d.weeksOut,
            dayOfWeek        = d.dayOfWeek.getValue,
            `type`           = WorkoutType.toDb(d.workoutType),
            targetDistanceM  = d.targetDistanceM,
            targetDurationS  = d.targetDurationS,
            targetVertM      = d.targetVertM,
            intensity        = d.intensity,
            notes            = d.notes
          ))
        )))
  }

  private val createPlanLogic = createPlan.serverLogic { req =>
    TemplateCatalog.find(req.templateKey) match
      case None    => IO.pure(Left(ApiError(s"unknown template: ${req.templateKey}")))
      case Some(t) =>
        for
          raceId <- raceRepo.insert(
                      name      = req.raceName,
                      raceDate  = req.raceDate,
                      distanceM = req.distanceM.getOrElse(t.defaultDistanceM),
                      vertM     = req.vertM.getOrElse(t.defaultVertM),
                      location  = req.location,
                      notes     = req.notes
                    )
          planId <- planRepo.insertPlan(raceId, athleteId, t.key)
          n      <- Generator.regenerate(planRepo, planId, req.raceDate, t.template)
          _      <- athleteRepo.setActivePlanId(athleteId, planId)
        yield Right(CreatePlanResponse(planId.value, raceId.value, n))
  }

  private val listPlansLogic = listPlans.serverLogic { _ =>
    for
      active <- athleteRepo.getActivePlanId(athleteId)
      rows   <- planRepo.listWithRaces(athleteId)
    yield Right(rows.map { r =>
      PlanSummaryView(
        planId       = r.planId.value,
        raceId       = r.raceId.value,
        templateKey  = r.templateKey,
        templateName = TemplateCatalog.find(r.templateKey).map(_.name),
        raceName     = r.raceName,
        raceDate     = r.raceDate,
        distanceM    = r.distanceM,
        vertM        = r.vertM,
        location     = r.location,
        workoutCount = r.workoutCount,
        generatedAt  = DateTimeFormatter.ISO_INSTANT.format(r.generatedAt),
        isActive     = active.exists(_.value == r.planId.value)
      )
    })
  }

  private val activatePlanLogic = activatePlan.serverLogic { id =>
    val pid = PlanId(id)
    planRepo.findPlanById(pid).flatMap {
      case None    => IO.pure(Left(ApiError(s"no plan with id $id")))
      case Some(_) => athleteRepo.setActivePlanId(athleteId, pid).as(Right(OkResponse(true)))
    }
  }

  private val deletePlanLogic = deletePlan.serverLogic { id =>
    planRepo.findPlanById(PlanId(id)).flatMap {
      case None       => IO.pure(Left(ApiError(s"no plan with id $id")))
      // Deleting the race cascades to training_plan → planned_workout → workout_match.
      // athlete.active_plan_id is ON DELETE SET NULL (V2), so no extra work needed.
      case Some(plan) => raceRepo.delete(plan.raceId).as(Right(OkResponse(true)))
    }
  }

  // -- helpers --------------------------------------------------------------

  private def toView(p: PlannedWorkout, m: Option[WorkoutMatch]): PlannedWorkoutView =
    PlannedWorkoutView(
      id              = p.id.value,
      date            = p.date,
      `type`          = WorkoutType.toDb(p.workoutType),
      targetDistanceM = p.targetDistanceM,
      targetDurationS = p.targetDurationS,
      targetVertM     = p.targetVertM,
      intensity       = p.intensity,
      notes           = p.notes,
      matchStatus     = m.map(x => MatchStatus.toDb(x.status)).getOrElse("pending"),
      matchedActivityId = m.flatMap(_.activityId).map(_.value)
    )

  private def toActivityView(a: Activity): ActivityView =
    ActivityView(
      id            = a.id.value,
      source        = ActivitySource.toDb(a.source),
      externalId    = a.externalId,
      startedAt     = DateTimeFormatter.ISO_INSTANT.format(a.startedAt),
      distanceM     = a.distanceM,
      movingSeconds = a.movingSeconds,
      elevationGainM= a.elevationGainM,
      avgHr         = a.avgHr,
      activityType  = a.activityType,
      name          = a.name
    )

  private def toSummary(t: PlanTemplate): TemplateSummary = TemplateSummary(
    key              = t.key,
    name             = t.name,
    description      = t.description,
    raceCategory     = t.raceCategory,
    terrain          = t.terrain,
    level            = t.level,
    weeks            = t.weeks,
    defaultDistanceM = t.defaultDistanceM,
    defaultVertM     = t.defaultVertM
  )

  // -- assembly -------------------------------------------------------------

  val serverEndpoints: List[ServerEndpoint[Any, IO]] = List(
    getCurrentRaceLogic,
    getPlanLogic,
    getWorkoutByDateLogic,
    regeneratePlanLogic,
    updateWorkoutLogic,
    listActivitiesLogic,
    createActivityLogic,
    rematchActivityLogic,
    listTemplatesLogic,
    getTemplateLogic,
    createPlanLogic,
    listPlansLogic,
    activatePlanLogic,
    deletePlanLogic
  )

  def httpRoutes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(serverEndpoints)
