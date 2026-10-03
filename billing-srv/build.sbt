scalaVersion := "3.9.0"
name := "billing-server"

val zioMainVersion = "2.1.26"
val zioConfigVersion = "4.0.8"
val testcontainersScalaVersion = "0.43.0"
val circeVersion = "0.14.14"

lazy val root =   Seq(
      //You can add library dependencies here, for example,
      //"org.scalatest" %% "scalatest" % "3.2.19" % Test,
      //"org.scalameta" %% "munit" % "1.2.3" % Test
      // ZIO
      "dev.zio" %% "zio" % zioMainVersion,

      // ZIO Config
      "dev.zio" %% "zio-config" % zioConfigVersion,
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
      "dev.zio" %% "zio-config-refined" % zioConfigVersion,

      // ZIO Test
      "dev.zio" %% "zio-test" % zioMainVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioMainVersion % Test,
      "dev.zio" %% "zio-test-magnolia" % zioMainVersion % Test,

      // ZIO HTTP
      "dev.zio" %% "zio-http" % "3.0.1",

      // Circe
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,

      // Database
      "org.postgresql" % "postgresql" % "42.7.7",
      "com.zaxxer" % "HikariCP" % "6.3.0",
      "org.liquibase" % "liquibase-core" % "4.33.0",
      "io.getquill" %% "quill-jdbc-zio" % "4.6.0.1",

      // logging
      "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
      "ch.qos.logback" % "logback-classic" % "1.5.18"

    )

