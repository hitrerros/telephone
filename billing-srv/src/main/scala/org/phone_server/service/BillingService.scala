package org.phone_server.service

import org.phone_commons.{AuthorisedPhoneClient, PhoneClient}
import org.phone_server.AddresseeStatus.*
import org.phone_server.ConnectionStatus.{DROPPED, ESTABLISHED}
import org.phone_server.cqrs.{ConnectionDropped, ConnectionEstablished, EventStore}
import org.phone_server.repository.CheckInRepository
import org.phone_server.{AddresseeStatus, ConnectionStatus}
import zio.{Task, UIO, ZIO, ZLayer}

import java.util.UUID

type SessionId = java.util.UUID

trait BillingService  {
   def dial(addressee : String, client : UUID) : UIO[Either[AddresseeStatus, (ConnectionStatus, SessionId)]]
   def drop(sessionId : SessionId) : UIO[Either[AddresseeStatus, (ConnectionStatus, SessionId)]]
}

class BillingServiceImpl(repo: CheckInRepository, eventStore: EventStore) extends BillingService {

  def dial(addressee: String, client: UUID): UIO[Either[AddresseeStatus, (ConnectionStatus, SessionId)]] = {
    (for {
      addresseeId <- repo.findAddresseeByNumber(addressee)
      result <- addresseeId match {
        case Some(recipient) =>
          repo.establishConnection(client, recipient)
            .flatMap(sessionId =>
              connectionEstablishedCommandHandler(client, recipient, sessionId) *>
              ZIO.succeed(Right((ESTABLISHED, sessionId))))
            .orElse(ZIO.succeed(Left(BUSY)))
        case None => ZIO.succeed(Left(NOT_REGISTERED))
      }
    } yield (result)).catchAll(_ => ZIO.succeed(Left(CONNECTION_ERROR)))
  }



  def drop(sessionId : SessionId) : UIO[Either[AddresseeStatus, (ConnectionStatus, SessionId)]] = {
    repo
      .dropConnection(sessionId)
      .flatMap(_ =>
        connectionDroppedCommandHandler(sessionId) *>
        ZIO.succeed(Right((DROPPED,sessionId))))
      .orElse(ZIO.succeed(Left(DISCONNECTION_ERROR)))
  }

   // CQRS command handlers
   private def connectionEstablishedCommandHandler(client: UUID, addressee: UUID,sessionId : UUID): Task[Unit] = {
     val event = ConnectionEstablished(sessionId = sessionId, from = client, to = addressee, fromName = "", toName = "")
     eventStore.append(sessionId = sessionId, expectedVersion = 0, events = List(event))
   }

  private def connectionDroppedCommandHandler(sessionId: UUID): Task[Unit] = {
    val event = ConnectionDropped(sessionId = sessionId, from = UUID.randomUUID(), to = UUID.randomUUID(),
      fromName = "", toName = "", duration = 0L)
    eventStore.append(sessionId = sessionId, expectedVersion = 0, events = List(event))
  }
}

object BillingService {
  val layer: ZLayer[CheckInRepository & EventStore, Nothing, BillingService] = ZLayer({
    for {
      checkInRepository <- ZIO.service[CheckInRepository]
      eventStore <- ZIO.service[EventStore]
      billingService <- ZIO.succeed(new BillingServiceImpl(checkInRepository, eventStore))
    } yield billingService
  })
}





