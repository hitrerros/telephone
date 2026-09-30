package org.phone_server.repository

import liquibase.Liquibase
import zio.{ULayer, ZIO, ZLayer}

trait LiquibaseService {
  def performMigration: ZIO[Liquibase, Throwable, Unit]

}

class LiquibaseServiceImpl extends LiquibaseService {
  def performMigration: zio.ZIO[liquibase.Liquibase, Throwable, Unit] =
    ZIO.serviceWithZIO[Liquibase] { liquibase =>
      ZIO.attempt(liquibase.update(""))
    }
}

object MigrationRunner {
  def performMigration: ZIO[Liquibase & LiquibaseService, Throwable, Unit] =
    ZIO.serviceWithZIO[LiquibaseService](_.performMigration)

  def layer: ULayer[LiquibaseService] =
    ZLayer.succeed(new LiquibaseServiceImpl())
}
