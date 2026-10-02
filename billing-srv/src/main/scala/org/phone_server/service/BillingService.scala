package org.phone_server.service

import org.phone_commons.{AuthorisedPhoneClient, PhoneClient}
import org.phone_server.AddresseeStatus.*
import org.phone_server.ConnectionStatus.{DROPPED, ESTABLISHED}
import org.phone_server.repository.CheckInRepository
import org.phone_server.{AddresseeStatus, ConnectionStatus}
import zio.{Task, UIO, ZIO, ZLayer}

import java.util.UUID

type SessionId = java.util.UUID

trait BillingService  {
   def checkIn(dto : PhoneClient) : Task[Option[AuthorisedPhoneClient]]
   def dial(addressee : String, client : UUID) : UIO[Either[AddresseeStatus, (ConnectionStatus, SessionId)]]
   def drop(sessionId : SessionId) : UIO[Either[AddresseeStatus, (ConnectionStatus, SessionId)]]
}

class BillingServiceImpl(repo: CheckInRepository) extends BillingService {
  def checkIn(dto: PhoneClient): Task[Option[AuthorisedPhoneClient]] = repo.checkIn(dto)

  def dial(addressee: String, client: UUID): UIO[Either[AddresseeStatus, (ConnectionStatus, SessionId)]] = {
    (for {
      addressee <- repo.findAddresseeByNumber(addressee)
      result <- addressee match {
        case Some(uuid) =>
          repo.establishConnection(client, uuid)
            .flatMap(uuid => ZIO.succeed(Right((ESTABLISHED, uuid))))
            .orElse(ZIO.succeed(Left(BUSY)))
        case None => ZIO.succeed(Left(NOT_REGISTERED))
      }
    } yield (result)).catchAll(_ => ZIO.succeed(Left(CONNECTION_ERROR)))
  }

  def drop(sessionId : SessionId) : UIO[Either[AddresseeStatus, (ConnectionStatus, SessionId)]] = {
    repo
      .dropConnection(sessionId)
      .flatMap(_ => ZIO.succeed(Right((DROPPED,sessionId))))
      .orElse(ZIO.succeed(Left(DISCONNECTION_ERROR)))
  }
}

object BillingService  {
  val layer : ZLayer[CheckInRepository,Nothing,BillingService] =
    ZLayer.fromFunction(v => new  BillingServiceImpl(v))
}





