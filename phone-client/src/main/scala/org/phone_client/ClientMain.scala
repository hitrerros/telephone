package org.phone_client

import cats.effect.kernel.Ref
import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.comcast.ip4s.{Host, Port}
import org.http4s.ember.server.EmberServerBuilder
import org.phone_client.api.ApiRoutes

object ClientMain extends IOApp {

  def run(args: List[String]): IO[ExitCode] = (for {
    sessionData <- Resource.eval(
      Ref.of[IO, Map[String,String]](Map.empty)
    )
    port = args.collectFirst { case v if (v.matches("port=.+")) => v.replaceFirst(".+=", "") }.getOrElse("8081").toInt
    _ <- Resource.eval(IO.println(s"Client start on port ${port}"))
    
    srv <- EmberServerBuilder
      .default[IO]
      .withHost(Host.fromString("localhost").get)
      .withPort(Port.fromInt(port).get)
      .withHttpApp(ApiRoutes.routes(sessionData).orNotFound)
      .build
  } yield (srv)).useForever

}
