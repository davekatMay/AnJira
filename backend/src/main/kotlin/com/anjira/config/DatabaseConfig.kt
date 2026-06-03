package com.anjira.config

import com.anjira.db.GroupMemberTable
import com.anjira.db.GroupTable
import com.anjira.db.MeetingParticipantTable
import com.anjira.db.MeetingTable
import com.anjira.db.RefreshTokenTable
import com.anjira.db.SubtaskTable
import com.anjira.db.TaskTable
import com.anjira.db.UserTable
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

        transaction {
            SchemaUtils.drop(UserTable, GroupTable, GroupMemberTable, TaskTable, SubtaskTable, MeetingTable, MeetingParticipantTable, RefreshTokenTable)
            SchemaUtils.create(UserTable, GroupTable, GroupMemberTable, TaskTable, SubtaskTable, MeetingTable, MeetingParticipantTable, RefreshTokenTable)
            logger.info("Database schema initialized")
        }
    }
}
