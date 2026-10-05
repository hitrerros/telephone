package org.phone_server

import org.phone_server.api.RootApi.*
import org.phone_server.cqrs.EventStore
import org.phone_server.repository.*
import org.phone_server.service.{BillingService, CheckInService}
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
      _ <- Server
        .serve(apiRoutes)
        .provide(Server.default,
                 BillingService.layer,
                 CheckInRepository.layer,
                 CheckInService.layer,
                 EventStore.layer)
    } yield ()
}
