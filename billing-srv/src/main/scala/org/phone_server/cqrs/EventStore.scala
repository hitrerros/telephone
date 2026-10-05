package org.phone_server.cqrs

import io.circe.generic.auto.*
import io.circe.parser.decode
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
                               eventType: String,
                               version: Int,
                               occurredAt: Instant,
                               producer: String,
                               payload: String
                               )

class EventStoreImpl extends EventStore {
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
            case "ConnectionEstablished" => decode[ConnectionEstablished](payLoad)
            case "ConnectionDropped" => decode[ConnectionDropped](payLoad)
          }
        }.collect { case Right(event) => event }
      )

    } yield v.toVector
  }

  override def append(sessionId: SessionId, expectedVersion: Long, events: List[BillingEvent]): Task[Unit] = {
     ???
  }
}

object EventStore {
  val layer : ULayer[EventStore] = ZLayer.succeed(new EventStoreImpl)
}