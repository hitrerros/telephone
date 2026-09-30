package org.phone_server

import com.zaxxer.hikari.HikariDataSource
import io.getquill.*
import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import zio.{ZIO, ZLayer}

import javax.sql.DataSource

package object repository {

  object ctx extends PostgresZioJdbcContext(NamingStrategy(SnakeCase, Escape, Literal))

  def hikariDS: HikariDataSource = {
    val ds = new HikariDataSource()
    ds.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres?currentSchema=billing")
    ds.setUsername("postgres")
    ds.setPassword("1")
    ds
  }

    val dsLayer: ZLayer[Any, Throwable, DataSource] =
      ZLayer.scoped {
        ZIO.acquireRelease(
          ZIO.attempt(hikariDS).map(ds => ds: DataSource)
        ) {
          case ds: HikariDataSource => ZIO.attempt(ds.close()).orDie
          case _ => ZIO.unit
        }
      }

    val liquibaseLayer: ZLayer[DataSource, Throwable, Liquibase] =
      ZLayer.scoped {
        for {
          ds <- ZIO.service[DataSource]
          conn <- ZIO.acquireRelease(ZIO.attempt(ds.getConnection))(c => ZIO.attempt(c.close()).orDie)
          jdbc = new JdbcConnection(conn)
          accessor <- ZIO.acquireRelease(
            ZIO.attempt(new ClassLoaderResourceAccessor())
          )(a => ZIO.attempt(a.close()).orDie)
          liquibase <- ZIO.acquireRelease(
            ZIO.attempt(new Liquibase("liquibase/changelog.yml", accessor, jdbc))
          )(_ => ZIO.unit)
        } yield liquibase
      }

}









