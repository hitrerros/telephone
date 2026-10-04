ThisBuild / scalaVersion := "3.8.4"

lazy val billingServer = project
  .in(file("billing-srv"))

lazy val phoneClient = project
  .in(file("phone-client"))