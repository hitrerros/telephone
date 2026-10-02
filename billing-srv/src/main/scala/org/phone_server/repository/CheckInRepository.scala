package org.phone_server.repository

import io.getquill.*
import org.phone_commons.{AuthorisedPhoneClient, PhoneClient}
import org.phone_server.{ActiveSession, BillingRecord}
import zio.{Task, ULayer, ZIO, ZLayer}

import java.time.LocalDateTime
import java.util.UUID

trait CheckInRepository  {
  def checkIn(dto : PhoneClient) : Task[Option[AuthorisedPhoneClient]]
  def findAddresseeByNumber(addressee: String): Task[Option[UUID]]
  def establishConnection(from: UUID, to : UUID): Task[UUID]
  def dropConnection(sessionId : UUID): Task[Unit]
}

class CheckInRepositoryImpl extends CheckInRepository {
  import ctx.*

  inline def clientsSchema: Quoted[EntityQuery[AuthorisedPhoneClient]] = quote {
    querySchema[AuthorisedPhoneClient]("clients")
  }

  inline def activeSessionsSchema: Quoted[EntityQuery[ActiveSession]] = quote {
    querySchema[ActiveSession]("active_calls")
  }

  inline def billingSchema: Quoted[EntityQuery[BillingRecord]] = quote {
    querySchema[BillingRecord]("billing")
  }
  
  override def checkIn(dto : PhoneClient): Task[Option[AuthorisedPhoneClient]] =
    (for {
      _ <- ctx.run(
        quote {
          clientsSchema
            .insert(_.phoneNumber -> lift(dto.phoneNumber),_.name -> lift(dto.name))
            .onConflictIgnore
        }
      )
      result <- ctx.run(clientsSchema.filter(_.phoneNumber == lift(dto.phoneNumber))).map(_.headOption)
    } yield result).provide(dsLayer)

  override def findAddresseeByNumber(addressee: String): Task[Option[UUID]] = {
    ctx.run(quote {
        clientsSchema.filter(_.phoneNumber == lift(addressee)).map(_.id)
      })
      .map(_.headOption).provide(dsLayer)
  }

  override def establishConnection(from: UUID, to: UUID): Task[UUID] = {
    val currentTime = LocalDateTime.now

    ctx.transaction {
      for {
        sessionId <- ctx.run(quote {
          billingSchema.insert(_.to -> lift(to), _.from -> lift(from), _.startTime -> lift(currentTime)).returning(_.sessionId)
        })
        _ <- ctx.run(quote {
          activeSessionsSchema.insertValue(lift(ActiveSession(id = to, from = from, startTime = currentTime, sessionId = sessionId)))
        })

      } yield sessionId
    }.provide(dsLayer)
  }

  override def dropConnection(sessionId : UUID): Task[Unit] = {
    val endTime = LocalDateTime.now

    ctx.transaction {
      for {
        _ <- ctx.run(quote {
          activeSessionsSchema.filter(_.sessionId == lift(sessionId)).delete
        })
        _ <- ctx.run(quote {
          billingSchema.filter(_.sessionId==lift(sessionId)).update(_.endTime -> lift(endTime))
        })

      } yield ()
    }.provide(dsLayer)
  }
} 

object CheckInRepository {
  val layer : ULayer[CheckInRepository] = ZLayer.succeed(new CheckInRepositoryImpl)
}
