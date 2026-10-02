package org.phone_client

import cats.effect.kernel.Ref
import cats.effect.{IO, IOApp, Resource}
import com.comcast.ip4s.{Host, Port}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.phone_client.api.ApiRoutes
import org.phone_commons.AuthorisedPhoneClient

object ClientMain extends IOApp.Simple {
  
  val s: Resource[IO, Server] = for {
    sessionData <- Resource.eval(Ref.of[IO, Set[AuthorisedPhoneClient]](Set.empty))
    
    srv <- EmberServerBuilder
      .default[IO]
      .withHost(Host.fromString("localhost").get)
      .withPort(Port.fromInt(8081).get)
      .withHttpApp(ApiRoutes.routes(sessionData).orNotFound)
      .build
      } yield (srv)
  
  def run: IO[Unit] = s.useForever  
        
}
