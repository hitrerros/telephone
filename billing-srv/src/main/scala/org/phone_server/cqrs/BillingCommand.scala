package org.phone_server.cqrs

import java.util.UUID

type SessionId = UUID

sealed trait BillingCommand

case class Dial(addressee: String, client: UUID) extends BillingCommand
case class Drop(sessionId: SessionId) extends BillingCommand