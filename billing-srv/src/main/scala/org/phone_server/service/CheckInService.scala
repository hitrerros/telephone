package org.phone_server.service

import org.phone_commons.{AuthorisedPhoneClient, PhoneClient}
import org.phone_server.repository.CheckInRepository
import zio.{Task, ZLayer}

trait CheckInService  {
   def checkIn(dto : PhoneClient) : Task[Option[AuthorisedPhoneClient]]
}

class CheckInServiceImpl(repo : CheckInRepository)  extends CheckInService {
   def checkIn(dto: PhoneClient): Task[Option[AuthorisedPhoneClient]] = repo.checkIn(dto)
}

object CheckInService  {
  val layer : ZLayer[CheckInRepository,Nothing,CheckInService] =
    ZLayer.fromFunction(v => new  CheckInServiceImpl(v))
}





