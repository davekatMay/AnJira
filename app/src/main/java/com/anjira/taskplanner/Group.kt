package com.anjira.taskplanner.domain.model

data class GroupMember(
    val userId: Int,
    val username: String,
    val email: String,
    val role: String,
    val createdAt: String,
    val updatedAt: String
)

data class Group(
    val id: Int,
    val name: String,
    val description: String?,
    val avatar: String?,
    val inviteCode: String,
    val createdBy: String,
    val members: List<GroupMember>,
    val createdAt: String,
    val updatedAt: String
)
