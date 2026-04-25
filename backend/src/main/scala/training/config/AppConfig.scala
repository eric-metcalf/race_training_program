package training.config

import cats.effect.IO
import cats.syntax.all.*
import ciris.*

final case class DatabaseConfig(
    url: String,
    user: String,
    password: Secret[String]
):
  /** Normalize Railway-style `postgres://user:pass@host:port/db?sslmode=require`
    * URLs into the `jdbc:postgresql://...` shape doobie's HikariTransactor needs.
    * Already-jdbc URLs pass through. User/password override anything in the URL.
    */
  def jdbcUrl: String =
    if url.startsWith("jdbc:") then url
    else if url.startsWith("postgres://") || url.startsWith("postgresql://") then
      val u    = java.net.URI.create(url)
      val q    = Option(u.getQuery).map("?" + _).getOrElse("")
      val port = if u.getPort < 0 then 5432 else u.getPort
      s"jdbc:postgresql://${u.getHost}:$port${u.getPath}$q"
    else url

final case class StravaConfig(
    clientId: String,
    clientSecret: Secret[String],
    verifyToken: Secret[String]
)

final case class AppConfig(
    httpPort: Int,
    appUrl: String,
    athleteId: Long,
    db: DatabaseConfig,
    strava: StravaConfig
)

object AppConfig:

  private val cfg: ConfigValue[Effect, AppConfig] =
    (
      // PORT is what Railway/Heroku/most PaaSes auto-assign; honor it first,
      // then HTTP_PORT (our local-dev convention), then default.
      (env("PORT").as[Int] or env("HTTP_PORT").as[Int]).default(8081),
      env("APP_URL").default("http://localhost:8081"),
      env("ATHLETE_ID").as[Long].default(1L),
      (
        env("DATABASE_URL").default("jdbc:postgresql://localhost:5432/training"),
        env("DATABASE_USER").default("training"),
        env("DATABASE_PASSWORD").default("training").secret
      ).parMapN(DatabaseConfig.apply),
      (
        env("STRAVA_CLIENT_ID").default(""),
        env("STRAVA_CLIENT_SECRET").default("").secret,
        env("STRAVA_VERIFY_TOKEN").default("dev-verify-token").secret
      ).parMapN(StravaConfig.apply)
    ).parMapN(AppConfig.apply)

  def load: IO[AppConfig] = cfg.load[IO]
