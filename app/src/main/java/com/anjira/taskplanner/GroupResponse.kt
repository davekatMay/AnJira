package com.anjira.taskplanner.data.remote.dto

data class GroupResponse(
    val id: Int,
    val name: String,
    val description: String?,
    val avatar: String?,
    val inviteCode: String,
    val createdBy: String,
    val members: List<GroupMemberResponse>,
    val createdAt: String,
    val updatedAt: String
)
