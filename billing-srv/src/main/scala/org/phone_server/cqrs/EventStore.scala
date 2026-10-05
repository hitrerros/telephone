package org.phone_server.cqrs

import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*

import io.getquill.*
import org.phone_server.repository.{ctx, dsLayer}
import zio.{Task, ULayer, ZIO, ZLayer}

import java.time.Instant
import java.util.UUID

trait EventStore {
  def load(sessionId: SessionId): Task[Vector[BillingEvent]]
  def append(sessionId: SessionId,expectedVersion: Long,events: List[BillingEvent]): Task[Unit]
}

final case class EventEnvelope(eventId: UUID,
                               aggregateId: UUID,
                               aggregateType: String,
                               version: Int,
                               eventType: String,
                               occurredAt: Instant,
                               producer: String,
                               payload: String
                               )

class EventStoreImpl extends EventStore {
  private val producer = "billing-server"
  import ctx.*

  private inline def eventsSchema: Quoted[EntityQuery[EventEnvelope]] = quote {
    querySchema[EventEnvelope]("events")
  }

  override def load(sessionId: SessionId): Task[Vector[BillingEvent]] = {
    for {
      queryResult <- ctx.run(
        quote {
          eventsSchema.filter(_.aggregateId == lift(sessionId)).map(v => (v.eventType, v.payload))
        }
      ).provide(dsLayer)

      v <- ZIO.succeed(
        queryResult.map { (eventType, payLoad) =>
          eventType match {
            case BillingEvent.ConnectionEstablished => decode[ConnectionEstablished](payLoad)
            case BillingEvent.ConnectionDropped => decode[ConnectionDropped](payLoad)
          }
        }.collect { case Right(event) => event }
      )
    } yield v.toVector
  }

  override def append(
                       sessionId: SessionId,
                       expectedVersion: Long,
                       events: List[BillingEvent]
                     ): Task[Unit] = {

    val envelopes = events.zipWithIndex.map { case (event, index) =>
      event match {
        case e: ConnectionEstablished =>
          EventEnvelope(
            eventId = UUID.randomUUID(),
            aggregateId = sessionId,
            aggregateType = "Billing",
            eventType = BillingEvent.ConnectionEstablished,
            version = (expectedVersion + index + 1).toInt,
            occurredAt = Instant.now(),
            producer = producer,
            payload = e.asJson.noSpaces
          )

        case e: ConnectionDropped =>
          EventEnvelope(
            eventId = UUID.randomUUID(),
            aggregateId = sessionId,
            aggregateType = "Billing",
            eventType = BillingEvent.ConnectionDropped,
            version = (expectedVersion + index + 2).toInt,
            occurredAt = Instant.now(),
            producer = producer,
            payload = e.asJson.noSpaces
          )
      }
    }

    ZIO.foreachDiscard(envelopes){
      ( event : EventEnvelope )  =>
        ctx.run(eventsSchema.insertValue(lift(event))).provide(dsLayer)
    }
  }

}

object EventStore {
  val layer : ULayer[EventStore] = ZLayer.succeed(new EventStoreImpl)
}