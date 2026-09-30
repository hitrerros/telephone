package org.phone_server

import org.phone_server.repository.{CheckInRepository, LiquibaseService, MigrationRunner, dsLayer, liquibaseLayer}
import zio.ZIOAppDefault
import zio.*
import zio.http.*
import org.phone_server.api.CheckInApi.*
import org.phone_server.service.CheckInService

object ServerMain extends ZIOAppDefault {

  def run: zio.ZIO[Any, Throwable, Unit] =
    for {
      _ <- MigrationRunner.performMigration.provide(
        liquibaseLayer,
        MigrationRunner.layer,
        dsLayer
      )

      _ <- Server.serve(apiRoutes).provide(Server.default,CheckInService.layer,CheckInRepository.layer)
    } yield ()

}
