package org.phone_client.api

import cats.effect.IO
import cats.effect.kernel.Ref
import org.http4s.Status.Ok
import org.http4s.dsl.io.{Ok, *}
import org.http4s.headers.`Content-Type`
import org.http4s.server.staticcontent.resourceServiceBuilder
import org.http4s.{HttpRoutes, MediaType, StaticFile, Status}
import org.phone_client.service.ServerCommunicationService.*
import org.phone_client.service.{ServerCommunicationService, UIService, clientId}
import org.phone_commons.AuthorisedPhoneClient

object ApiRoutes {

  private val comService = implicitly[ServerCommunicationService[IO]]
  type Session[F[_]] = Ref[F, Map[String,String]]

  private def responseToSessionData(client: AuthorisedPhoneClient) : Map[String,String] = {
    Map(clientId ->  client.id.toString, "phone_number" -> client.phoneNumber, "name" -> client.name  )
  }

  def routes(session: Session[IO]): HttpRoutes[IO] = HttpRoutes.of {
  // UI
    case GET -> Root   =>
      Ok(UIService.getIndexTemplateHtml)
        .map(_.withContentType(`Content-Type`(MediaType.text.html)))

    case req @ GET -> Root / "static" / path =>
      StaticFile
        .fromResource(s"static/$path", Some(req))
        .getOrElseF(NotFound())

      // Business logic     
      
    case GET -> Root / "register" / phoneNumber / name =>
      comService.register(phoneNumber = phoneNumber, alias = name).flatMap {
        case Left(error) => Status.BadRequest(error.toString)
        case Right(response) =>
          session.update(_ => responseToSessionData(response))
            *> Ok (s"Client [${response.name}] has been signed into the app")
      }

    case GET -> Root / "dial" / addressee => 
       comService.dial(addressee,session).flatMap(v => Ok(v))

    case GET -> Root / "drop"  =>
      comService.drop(session).flatMap(v => Ok(v))


  }
}
