package org.phone_server

import org.phone_server.api.RootApi.*
import org.phone_server.repository.*
import org.phone_server.service.BillingService
import zio.*
import zio.http.*

object ServerMain extends ZIOAppDefault {

  def run: zio.ZIO[Any, Throwable, Unit] =
    for {
      _ <- MigrationRunner.performMigration.provide(
        liquibaseLayer,
        MigrationRunner.layer,
        dsLayer
      )

      _ <- Server.serve(apiRoutes).provide(Server.default,BillingService.layer,CheckInRepository.layer)
    } yield ()

}
