package org

import java.time.LocalDateTime
import java.util.UUID

package object phone_server {
  enum AddresseeStatus {
    case  NOT_REGISTERED,BUSY,READY,CONNECTION_ERROR,DISCONNECTION_ERROR 
  }
  enum ConnectionStatus  {
    case ESTABLISHED, DROPPED
  }
  
  case class ActiveSession(id: UUID, from: UUID, startTime: LocalDateTime, sessionId : UUID)
  case class BillingRecord(sessionId: UUID, from: UUID, to: UUID, startTime: LocalDateTime, endTime: LocalDateTime)
}
