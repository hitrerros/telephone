package org.phone_server.repository

import io.getquill.*
import org.phone_commons.{AuthorisedPhoneClient, PhoneClient}
import zio.{Task, ULayer, ZLayer}

trait CheckInRepository  {
  def checkIn(dto : PhoneClient) : Task[Option[AuthorisedPhoneClient]]
}

class CheckInRepositoryImpl extends CheckInRepository {
  import ctx.*

  inline def clientsSchema: Quoted[EntityQuery[AuthorisedPhoneClient]] = quote {
    querySchema[AuthorisedPhoneClient]("clients")
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
} 

object CheckInRepository {
  val layer : ULayer[CheckInRepository] = ZLayer.succeed(new CheckInRepositoryImpl)
}
