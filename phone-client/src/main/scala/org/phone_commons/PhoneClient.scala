package org.phone_commons

import java.util.UUID


final case class PhoneClient(phoneNumber: String, name: String)

final case class AuthorisedPhoneClient(id: UUID, phoneNumber: String, name: String)

