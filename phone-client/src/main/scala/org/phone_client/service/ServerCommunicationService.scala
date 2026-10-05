package org.phone_client.service

import cats.Applicative
import cats.effect.{Async, IO}
import cats.implicits.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.headers.Authorization
import org.http4s.{AuthScheme, Credentials, Method, Request, Status, Uri}
import org.phone_client.api.ApiRoutes.Session
import org.phone_commons.{AuthorisedPhoneClient, PhoneClient}

sealed trait ValidationError
case object InvalidPhoneNumber extends ValidationError
case object AddresseeNotFound extends ValidationError
case object UnknownError extends ValidationError


sealed trait ServerCommunicationService [F[_]] {
  def register(phoneNumber : String, alias : String) : F[Either[ValidationError, AuthorisedPhoneClient]]
  def dial(addressee : String, session: Session[F]) : F[String]
  def drop(session: Session[F]) : F[String]
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

    override def register(phoneNumber: String, alias: String): F[Either[ValidationError, AuthorisedPhoneClient]] = {
        validateNumber(phoneNumber).flatMap {
          case Left(error) => Async[F].pure(Left(error))
          case Right(number) =>
            val request = Request[F](
              method = Method.POST,
              uri = Uri.fromString(s"${registerUrl}").toOption.get
            ).withEntity(PhoneClient(phoneNumber = number, name = alias))

            builder.use { client =>
              client.run(request).use { response =>
                response.status match {
                  case Status.Ok =>
                    response.as[AuthorisedPhoneClient].map(Right(_))
                  case _ =>
                    Applicative[F].pure(Left(UnknownError))
               }
              }
            }
        }
  }

    override def dial(addressee: String, session: Session[F]): F[String] = {
      for {
        current <- session.get
        request = Request[F](
          method = Method.POST,
          uri = Uri.fromString(s"$dialUrl$addressee").toOption.get
        ).putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, current(clientId))))
        
        response <- builder.use { client =>
          client.run(request).use { response =>
            response.status match {
              case Status.Ok =>
                response.as[String].map(Some(_))
              case _ =>
                Async[F].pure(None)
            }
          }
        }
        _ <- response match {
          case Some(value) =>  session.update(v => v + (sessionId -> value))
          case None => Async[F].unit
        }  
      } yield response.map(v=>s"Connection successfull $v").getOrElse("Connection not established")
    }

  override def drop(session: Session[F]): F[String] = {
      for {
        current <- session.get

        response <- current.get(sessionId) match {
          case Some(id) =>
            builder.use { client =>
              client.expect[String] {
                Request[F](
                  method = Method.POST,
                  uri = Uri.fromString(s"$dropUrl$id").toOption.get
                ).putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, current(clientId))))
              }
            }
          case None => "session id is missing".pure[F]
        }
      } yield response
    }
  }

  object ServerCommunicationService {
    implicit val comService: ServerCommunicationService[IO] = new ServerCommunicationServiceImpl[IO]
  }