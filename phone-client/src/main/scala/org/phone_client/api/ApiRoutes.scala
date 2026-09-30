package org.phone_client.api

import cats.effect.IO
import org.http4s.{HttpRoutes, Status}
import org.http4s.Status.Ok
import org.http4s.dsl.io.*
import org.phone_client.service.ServerCommunicationService
import org.phone_client.service.ServerCommunicationService.*

object ApiRoutes {

  private val comService  = implicitly[ServerCommunicationService[IO]]
  
  def routes : HttpRoutes[IO] = HttpRoutes.of{
    case GET -> Root / "register" / phoneNumber / name =>
      IO.println("hi ") *> comService.register(phoneNumber = phoneNumber, alias = name).flatMap{
        case Left(value) =>  Status.BadRequest(value.toString)
        case Right(value) =>  Ok(value.toString)
      }
    case GET -> Root / "health" =>
      Ok("success")
  }
}
