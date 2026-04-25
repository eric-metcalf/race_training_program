package training

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.semigroupk.*
import com.comcast.ip4s.*
import org.http4s.HttpRoutes
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.{Router, Server}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import training.config.AppConfig
import training.db.{Database, Migrations}
import training.http.{OpenApiRoutes, Routes, StaticRoutes}
import training.repo.{ActivityRepo, AthleteRepo, MatchRepo, PlanRepo, RaceRepo}

object Main extends IOApp:

  given Logger[IO] = Slf4jLogger.getLogger[IO]

  def run(args: List[String]): IO[ExitCode] =
    for
      cfg     <- AppConfig.load
      _       <- Logger[IO].info(s"Loaded config — http :${cfg.httpPort}, db ${cfg.db.url}")
      applied <- Migrations.run(cfg.db)
      _       <- Logger[IO].info(s"Flyway applied $applied new migration(s)")
      _       <- Database.transactor(cfg.db).use { xa =>
                   val raceRepo     = RaceRepo(xa)
                   val planRepo     = PlanRepo(xa)
                   val activityRepo = ActivityRepo(xa)
                   val matchRepo    = MatchRepo(xa)
                   val athleteRepo  = AthleteRepo(xa)
                   val routes       = Routes(cfg, raceRepo, planRepo, activityRepo, matchRepo, athleteRepo)
                   // Order matters: API + OpenAPI first, then static SPA as fallback
                   // (StaticRoutes always returns 200 with index.html on unknown paths
                   // so client-side routing works, so it must come last).
                   val app =
                     healthRoutes <+>
                     routes.httpRoutes <+>
                     OpenApiRoutes.routes <+>
                     StaticRoutes.routes
                   server(cfg, app).useForever
                 }
    yield ExitCode.Success

  private def healthRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "api" / "ping" => Ok("""{"status":"ok"}""")
  }

  private def server(cfg: AppConfig, app: HttpRoutes[IO]): Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      // Bind on the IPv6 wildcard so Railway's IPv6-only internal proxy can
      // reach us. On Linux this also accepts IPv4 connections via v4-mapped
      // addresses, so local dev (curl localhost:8081) keeps working.
      .withHost(ipv6"::")
      .withPort(Port.fromInt(cfg.httpPort).getOrElse(port"8081"))
      .withHttpApp(Router("/" -> app).orNotFound)
      .build
