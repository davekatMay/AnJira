package com.anjira.taskplanner.data.remote.dto

data class AddMemberRequest(
    val userId: Int,
    val role: String = "member"
)

data class RefreshRequest(
    val refreshToken: String
)

data class GroupUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val avatar: String? = null
)

data class JoinByCodeRequest(
    val inviteCode: String
)

data class UpdateRoleRequest(
    val role: String
)
