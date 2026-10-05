package org.phone_server.cqrs

import java.util.UUID

sealed trait BillingEvent

case class ConnectionEstablished(sessionId: SessionId, from: UUID, fromName: String,
                                 to: UUID, toName: String) extends BillingEvent

case class ConnectionDropped(sessionId: SessionId,
                             from: UUID, fromName: String,
                             to: UUID, toName: String,
                             duration: Long) extends BillingEvent

object BillingEvent {
   val ConnectionEstablished = "ConnectionEstablished"
   val ConnectionDropped = "ConnectionDropped"
}