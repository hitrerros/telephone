scalaVersion := "3.9.0"

val http4sversion = "0.23.37"
val circeVersion = "0.14.14"

lazy val root = rootProject
  .settings(
  libraryDependencies ++= Seq(
    //You can add library dependencies here, for example,
    //"org.scalatest" %% "scalatest" % "3.2.19" % Test,
    //"org.scalameta" %% "munit" % "1.2.3" % Test
    // Cats
    "org.typelevel" %% "cats-core" % "2.13.0",

    // Cats Effect
    "org.typelevel" %% "cats-effect" % "3.7.0",

    // HTTP4s
    "org.http4s" %% "http4s-ember-server" % http4sversion,
    "org.http4s" %% "http4s-ember-client" % http4sversion,
    "org.http4s" %% "http4s-dsl" % http4sversion,
    "org.http4s" %% "http4s-circe" % http4sversion,

    // Circe
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
  )
  )
