package org.phone_server.api

import io.circe.Encoder.*
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import org.phone_commons.{PhoneClient, given}
import org.phone_server.service.BillingService
import zio.*
import zio.http.*
import zio.http.Header.Authorization

import java.util.UUID

object RootApi {

  private def jsonResponse(json: String, status: Status = Status.Ok): Response =
    Response(
      status = status,
      headers = Headers(Header.ContentType(MediaType.application.json)),
      body = Body.fromString(json)
    )

  private def badRequest(message: String): Response =
    jsonResponse(Map("error" -> message).asJson.noSpaces, Status.BadRequest)

  private val registerRoute = handler((req: Request) =>
    (for {
      body <- req.body.asString
      dto <- ZIO.fromEither(decode[PhoneClient](body))
      billingService <- ZIO.service[BillingService]
      client <- billingService.checkIn(dto)
    } yield jsonResponse(client.get.asJson.noSpaces))
      .catchAll(error => ZIO.succeed(badRequest(error.getMessage)))
  )

  private val dialRoute =
    handler((addressee: String,req: Request ) =>
      for {
        billingService <- ZIO.service[BillingService]
        phoneId = req.headers.find(v => v.headerType == Header.Authorization)
        dialResult <- ZIO.fromOption(phoneId)
          .flatMap {
            v =>
              billingService
                .dial(addressee, UUID.fromString(v.renderedValue))
                .flatMap {
                  case Left(res) => ZIO.succeed(badRequest(res.toString))
                  case Right(res) => ZIO.succeed(jsonResponse(res.toString))
                }
          }
          .orElse(ZIO.succeed(badRequest("not authorized")))
      } yield dialResult
    )


  val apiRoutes: Routes[BillingService, Response] = {
    Routes(
      Method.GET / "health" -> handler(Response.text("ok!")),
      Method.POST / "register" -> registerRoute,
      Method.GET / "dial" / string("addressee") -> dialRoute
    )
  }
}
