package com.anjira.config

import com.anjira.db.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object DatabaseConfig {
    private val logger = LoggerFactory.getLogger("DatabaseConfig")

    fun init() {
        val url = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/taskplanner"
        val user = System.getenv("DB_USER") ?: "postgres"
        val password = System.getenv("DB_PASSWORD") ?: "postgres"

        logger.info("Connecting to database at $url")

        Database.connect(url, driver = "org.postgresql.Driver", user = user, password = password)

        val allTables = arrayOf(
            UserTable, GroupTable, GroupMemberTable, TaskTable, SubtaskTable,
            MeetingTable, MeetingParticipantTable, RefreshTokenTable,
            AnnouncementTable, PlaylistTable, PlaylistTrackTable, NotificationTable
        )

        transaction {
            SchemaUtils.drop(*allTables)
            SchemaUtils.create(*allTables)
            logger.info("Database schema initialized")
        }
    }
}
