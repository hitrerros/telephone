package org.phone_client.service

import cats.data.OptionT
import cats.effect.{Async, IO}
import cats.implicits.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.headers.Authorization
import org.http4s.{AuthScheme, Credentials, Method, Request, Uri}
import org.phone_client.api.ApiRoutes.Session
import org.phone_commons.{AuthorisedPhoneClient, PhoneClient}

sealed trait ValidationError
case object InvalidPhoneNumber extends ValidationError

sealed trait ServerCommunicationService [F[_]] {
  def register(phoneNumber : String, alias : String) : F[Either[ValidationError, AuthorisedPhoneClient]]
  def dial(addressee : String, session: Session[F]) : F[String]
}

final class ServerCommunicationServiceImpl[F[_] : Async] extends ServerCommunicationService[F] {
  private val builder = EmberClientBuilder.default[F].build


  private def validateNumber(phoneNumber: String): F[Either[ValidationError, String]] = {
    Async[F].delay {
      val convertedNumber = phoneNumber.replaceAll("\\D", "")
      if (convertedNumber.length != 5)
        Left(InvalidPhoneNumber)
      else
        Right(convertedNumber)
    }
  }

  def register(phoneNumber: String, alias : String):  F[Either[ValidationError, AuthorisedPhoneClient]] = {
    validateNumber(phoneNumber).flatMap{
      case Left(error) => Async[F].pure(Left(error))
      case Right(number) =>
                   val request = Request[F](
                               method = Method.POST,
                               uri = Uri.fromString(s"${registerUrl}").toOption.get
                             ).withEntity(PhoneClient(phoneNumber = number, name = alias))
                   builder.use { client => client.expect[AuthorisedPhoneClient](request).map(v => Right(v)) }
    }
  }

  override def dial(addressee: String, session: Session[F]): F[String] = {
    for {
      current <- session.get
      request = Request[F](
        method = Method.GET,
        uri = Uri.fromString(s"${dialUrl}/${addressee}").toOption.get
      ).putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, current.head.id.toString)))
      response <- builder.use { client => client.expect[String](request) }
    } yield response
  }
}

object ServerCommunicationService {
  implicit val comService: ServerCommunicationService[IO] = new ServerCommunicationServiceImpl[IO]
}