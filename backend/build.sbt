import com.typesafe.sbt.packager.docker._

ThisBuild / scalaVersion := "3.5.2"
ThisBuild / organization := "com.training"
ThisBuild / version      := "0.1.0"

val Versions = new {
  val http4s    = "0.23.28"
  val circe     = "0.14.10"
  val tapir     = "1.11.10"
  val doobie    = "1.0.0-RC5"
  val ciris     = "3.6.0"
  val flyway    = "10.20.1"
  val postgres  = "42.7.4"
  val hikari    = "5.1.0"
  val logback   = "1.5.12"
  val munit     = "1.0.2"
  val munitCe   = "2.0.0"
  val log4cats  = "2.7.0"
}

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    name := "race-training-backend",
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Wunused:all",
      "-Wvalue-discard",
      "-Xfatal-warnings"
    ),
    libraryDependencies ++= Seq(
      // http4s
      "org.http4s"    %% "http4s-ember-server" % Versions.http4s,
      "org.http4s"    %% "http4s-ember-client" % Versions.http4s,
      "org.http4s"    %% "http4s-circe"        % Versions.http4s,
      "org.http4s"    %% "http4s-dsl"          % Versions.http4s,
      // circe
      "io.circe"      %% "circe-core"          % Versions.circe,
      "io.circe"      %% "circe-generic"       % Versions.circe,
      "io.circe"      %% "circe-parser"        % Versions.circe,
      // tapir
      "com.softwaremill.sttp.tapir" %% "tapir-core"              % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"     % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"      % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-files"             % Versions.tapir,
      // doobie + postgres
      "org.tpolecat"   %% "doobie-core"        % Versions.doobie,
      "org.tpolecat"   %% "doobie-hikari"      % Versions.doobie,
      "org.tpolecat"   %% "doobie-postgres"    % Versions.doobie,
      "org.tpolecat"   %% "doobie-postgres-circe" % Versions.doobie,
      "org.postgresql" %  "postgresql"         % Versions.postgres,
      "com.zaxxer"     %  "HikariCP"           % Versions.hikari,
      // flyway
      "org.flywaydb"   %  "flyway-core"               % Versions.flyway,
      "org.flywaydb"   %  "flyway-database-postgresql" % Versions.flyway,
      // ciris config
      "is.cir"         %% "ciris"              % Versions.ciris,
      // logging
      "ch.qos.logback" %  "logback-classic"    % Versions.logback,
      "org.typelevel"  %% "log4cats-slf4j"     % Versions.log4cats,
      // tests
      "org.scalameta"  %% "munit"              % Versions.munit   % Test,
      "org.typelevel"  %% "munit-cats-effect"  % Versions.munitCe % Test
    ),
    // docker (for Railway)
    Docker / packageName  := "race-training-backend",
    dockerBaseImage       := "eclipse-temurin:21-jre",
    dockerExposedPorts    := Seq(8081),
    dockerUpdateLatest    := true,
    dockerCommands ++= Seq(
      Cmd("ENV", "HTTP_PORT=8081")
    ),
    // sbt-revolver: hot reload on save
    reStart / javaOptions += "-Dconfig.file=conf/application.conf",
    // run uses fork
    run / fork := true,
    Test / fork := true,
    Test / testFrameworks += new TestFramework("munit.Framework")
  )
