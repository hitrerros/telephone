package org.phone_server.api

import io.circe.Encoder._
import io.circe.generic.auto.deriveEncoder
import io.circe.syntax.*
import org.phone_commons.{PhoneClient, AuthorisedPhoneClient, given}
import org.phone_server.service.CheckInService
import zio.*
import zio.http.*
import zio.http.codec.PathCodec.string
import io.circe.generic.auto.*
import io.circe.parser.decode

object CheckInApi {

  private def jsonResponse(json: String, status: Status = Status.Ok): Response =
    Response(
      status = status,
      headers = Headers(Header.ContentType(MediaType.application.json)),
      body = Body.fromString(json)
    )

  private def badRequest(message: String): Response =
    jsonResponse(Map("error" -> message).asJson.noSpaces, Status.BadRequest)

  val apiRoutes: Routes[CheckInService, Response] =
    Routes(
      Method.GET / "health" -> handler(Response.text("ok!")),
      Method.POST / "register" -> handler((req: Request) =>
        (for {
          body <- req.body.asString
          dto <- ZIO.fromEither(decode[PhoneClient](body))
          checkInService <- ZIO.service[CheckInService]
          client <- checkInService.checkIn(dto)
        } yield jsonResponse(client.get.asJson.noSpaces))
          .catchAll(error => ZIO.succeed(badRequest(error.getMessage))
        )
      )
    )
}
