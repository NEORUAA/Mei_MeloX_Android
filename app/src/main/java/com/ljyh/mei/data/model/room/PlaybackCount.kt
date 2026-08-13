package com.ljyh.mei.data.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_count")
data class PlaybackCount(
    @PrimaryKey val songId: String,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0,
)
