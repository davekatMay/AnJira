package com.anjira.taskplanner.data.remote.dto

data class TrackResponse(
    val id: Int,
    val playlistId: Int,
    val trackId: String,
    val trackName: String,
    val artistName: String,
    val trackViewUrl: String,
    val artworkUrl100: String?,
    val previewUrl: String?,
    val sortOrder: Int,
    val createdAt: String
)
