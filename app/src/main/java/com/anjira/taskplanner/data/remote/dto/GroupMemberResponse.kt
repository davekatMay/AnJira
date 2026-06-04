package com.anjira.taskplanner.data.remote.dto

data class GroupMemberResponse(
    val userId: Int,
    val groupId: Int,
    val username: String,
    val email: String,
    val role: String,
    val createdAt: String,
    val updatedAt: String
)
