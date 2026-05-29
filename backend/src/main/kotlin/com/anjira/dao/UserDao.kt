package com.anjira.dao

import com.anjira.db.GroupMemberTable
import com.anjira.db.GroupTable
import com.anjira.db.MeetingParticipantTable
import com.anjira.db.MeetingTable
import com.anjira.db.SubtaskTable
import com.anjira.db.TaskTable
import com.anjira.db.UserTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class UserEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserEntity>(UserTable)

    var username by UserTable.username
    var passwordHash by UserTable.passwordHash
    var email by UserTable.email
}

class GroupEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<GroupEntity>(GroupTable)

    var name by GroupTable.name
    var description by GroupTable.description
    var createdBy by GroupTable.createdBy
}

class GroupMemberEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<GroupMemberEntity>(GroupMemberTable)

    var groupId by GroupMemberTable.groupId
    var userId by GroupMemberTable.userId
    var role by GroupMemberTable.role
}

class TaskEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TaskEntity>(TaskTable)

    var groupId by TaskTable.groupId
    var title by TaskTable.title
    var description by TaskTable.description
    var status by TaskTable.status
    var createdBy by TaskTable.createdBy
    var assignedTo by TaskTable.assignedTo
}

class SubtaskEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SubtaskEntity>(SubtaskTable)

    var taskId by SubtaskTable.taskId
    var title by SubtaskTable.title
    var isCompleted by SubtaskTable.isCompleted
}

class MeetingEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<MeetingEntity>(MeetingTable)

    var groupId by MeetingTable.groupId
    var title by MeetingTable.title
    var description by MeetingTable.description
    var dateTime by MeetingTable.dateTime
    var location by MeetingTable.location
    var createdBy by MeetingTable.createdBy
}

class MeetingParticipantEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<MeetingParticipantEntity>(MeetingParticipantTable)

    var meetingId by MeetingParticipantTable.meetingId
    var userId by MeetingParticipantTable.userId
}
