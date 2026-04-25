package training.db

import cats.effect.{IO, Resource}
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import training.config.DatabaseConfig

object Database:

  def transactor(cfg: DatabaseConfig): Resource[IO, HikariTransactor[IO]] =
    val hikari = new HikariConfig()
    hikari.setDriverClassName("org.postgresql.Driver")
    hikari.setJdbcUrl(cfg.jdbcUrl)
    hikari.setUsername(cfg.user)
    hikari.setPassword(cfg.password.value)
    hikari.setMaximumPoolSize(10)
    hikari.setPoolName("race-training-pool")
    HikariTransactor.fromHikariConfig[IO](hikari)
