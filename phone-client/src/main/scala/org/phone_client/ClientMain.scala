package org.phone_client

import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{Host, Port}
import org.http4s.ember.server.EmberServerBuilder
import org.phone_client.api.ApiRoutes

object ClientMain extends IOApp.Simple {

 private val server = EmberServerBuilder
   .default[IO]
   .withHost(Host.fromString("localhost").get)
   .withPort(Port.fromInt(8081).get)
   .withHttpApp(ApiRoutes.routes.orNotFound)
   .build
   .useForever


  def run: IO[Unit] = server
}
