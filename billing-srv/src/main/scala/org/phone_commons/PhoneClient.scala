package org.phone_commons

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

import java.util.UUID


final case class PhoneClient(phoneNumber: String, name: String)
final case class AuthorisedPhoneClient(id: UUID, phoneNumber: String, name: String)

given Encoder[AuthorisedPhoneClient] = deriveEncoder[AuthorisedPhoneClient]