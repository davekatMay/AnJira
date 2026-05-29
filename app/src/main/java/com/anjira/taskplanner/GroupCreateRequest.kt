package com.anjira.taskplanner.data.remote.dto

data class GroupCreateRequest(
    val name: String,
    val description: String? = null
)