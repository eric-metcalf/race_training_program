package training.db

import cats.effect.IO
import org.flywaydb.core.Flyway
import training.config.DatabaseConfig

object Migrations:

  def run(cfg: DatabaseConfig): IO[Int] = IO.blocking {
    Flyway
      .configure()
      .dataSource(cfg.jdbcUrl, cfg.user, cfg.password.value)
      .locations("classpath:db/migration")
      .baselineOnMigrate(true)
      .load()
      .migrate()
      .migrationsExecuted
  }
