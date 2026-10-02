package org.phone_client.api

import cats.effect.IO
import cats.effect.kernel.Ref
import org.http4s.Status.Ok
import org.http4s.dsl.io.*
import org.http4s.{HttpRoutes, Status}
import org.phone_client.service.ServerCommunicationService.*
import org.phone_client.service.{ServerCommunicationService, clientId}
import org.phone_commons.AuthorisedPhoneClient

object ApiRoutes {

  private val comService = implicitly[ServerCommunicationService[IO]]
  type Session[F[_]] = Ref[F, Map[String,String]]

  private def responseToSessionData(client: AuthorisedPhoneClient) : Map[String,String] = {
    Map(clientId ->  client.id.toString, "phone_number" -> client.phoneNumber, "name" -> client.name  )
  }

  def routes(session: Session[IO]): HttpRoutes[IO] = HttpRoutes.of {
    case GET -> Root / "health" =>
      Ok("success")

    case GET -> Root / "register" / phoneNumber / name =>
      comService.register(phoneNumber = phoneNumber, alias = name).flatMap {
        case Left(error) => Status.BadRequest(error.toString)
        case Right(response) =>
          session.update(_ => responseToSessionData(response))
            *> Ok (s"received user ${response.name}")
      }

    case GET -> Root / "dial" / addressee => 
       comService.dial(addressee,session).flatMap(v => Ok(v))

    case GET -> Root / "drop"  =>
      comService.drop(session).flatMap(v => Ok(v))


  }
}
