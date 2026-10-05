package org.phone_server.cqrs

import java.util.UUID

case class BillingState (sessionId: SessionId, from: UUID, to: UUID, connected: Boolean)

object BillingState {

  def applyEvent(state: Option[BillingState], event: BillingEvent): BillingState = {
    event match
      case ConnectionEstablished(sessionId, from, _, to, _) =>
        BillingState(sessionId = sessionId, from = from, to = to, connected = true)

      case ConnectionDropped(_,_,_,_,_,_) => state.get.copy(connected = false)
  }
}