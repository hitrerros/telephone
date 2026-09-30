package org.phone_client

import cats.effect.Async
import io.circe.generic.auto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}
import org.phone_commons._

import java.util.UUID

package object service {
  val registerUrl = "http://localhost:8080/register/"
  
  implicit def encodeClient[F[_] : Async]:  EntityEncoder[F,PhoneClient] = jsonEncoderOf[F,PhoneClient]
  implicit def decodeAuthorisedClient[F[_] : Async]: EntityDecoder[F, AuthorisedPhoneClient] = jsonOf[F, AuthorisedPhoneClient]
}
