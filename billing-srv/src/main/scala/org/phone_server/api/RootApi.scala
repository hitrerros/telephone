package org.phone_server.api

import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import org.phone_commons.{PhoneClient, given}
import org.phone_server.service.{BillingService,CheckInService}
import zio.*
import zio.http.*

import java.util.UUID

object RootApi {

  private def jsonResponse(json: String, status: Status = Status.Ok): Response =
    Response(
      status = status,
      headers = Headers(Header.ContentType(MediaType.application.json)),
      body = Body.fromString(json)
    )

  private def badRequest(message: String, status: Status = Status.BadRequest): Response =
    Response(
      status = status,
      headers = Headers(Header.ContentType(MediaType.text.plain)),
      body = Body.fromString(message)
    )

  private val registerRoute = handler((req: Request) =>
    (for {
      body <- req.body.asString
      dto <- ZIO.fromEither(decode[PhoneClient](body))
      checkInService <- ZIO.service[CheckInService]
      client <- checkInService.checkIn(dto)
    } yield jsonResponse(client.get.asJson.noSpaces))
      .catchAll(error => ZIO.succeed(badRequest(error.getMessage)))
  )

  private val dialRoute =
    handler((addressee: String,req: Request ) =>
      for {
        billingService <- ZIO.service[BillingService]
        phoneId = req.headers.find(v => v.headerName == "Authorization")
        dialResult <- ZIO.fromOption(phoneId)
          .flatMap {
            v =>
              billingService
                .dial(addressee, UUID.fromString(v.renderedValue.replaceFirst("Bearer ","")))
                .flatMap {
                  case Left(res) =>
                    ZIO.succeed(badRequest(res.toString,Status.NotFound))
                  case Right((_,sessionId)) => ZIO.succeed(jsonResponse(sessionId.toString))
                }
          }
          .orElse(ZIO.succeed(badRequest("not authorized")))
      } yield dialResult
    )

  private val dropRoute =
    handler((sessionId: String,req: Request ) =>
      for {
        billingService <- ZIO.service[BillingService]
        phoneId = req.headers.find(v => v.headerName == "Authorization")
        dialResult <- ZIO.fromOption(phoneId)
          .flatMap {
            v =>
              billingService
                .drop(UUID.fromString(sessionId))
                .flatMap {
                  case Left(res) => ZIO.succeed(badRequest(res.toString))
                  case Right(res) => ZIO.succeed(jsonResponse(res.toString))
                }
          }
          .orElse(ZIO.succeed(badRequest("not authorized")))
      } yield dialResult
    )

  val apiRoutes: Routes[BillingService & CheckInService, Response] = {
    Routes(
      Method.POST / "register" -> registerRoute,
      Method.POST / "dial" / string("addressee") -> dialRoute,
      Method.POST / "drop" / string("sessionId") -> dropRoute
    )
  }
}
