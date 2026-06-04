package com.anjira.config

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.postgresql.ds.PGSimpleDataSource
import org.slf4j.LoggerFactory

object DatabaseConfig {
    private val logger = LoggerFactory.getLogger("DatabaseConfig")

    fun init() {
        val url = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/taskplanner"
        val user = System.getenv("DB_USER") ?: "postgres"
        val password = System.getenv("DB_PASSWORD") ?: "postgres"

        logger.info("Connecting to database at $url")

        val dataSource = PGSimpleDataSource().apply {
            setUrl(url)
            setUser(user)
            setPassword(password)
        }

        Database.connect(dataSource)

        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()

        logger.info("Database schema initialized")
    }
}
