package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Phase 4.2: Local Private Override Layer stored exclusively in on-device Room SQLite.
 * Never synced or written to Firebase.
 */
@Entity(tableName = "local_aliases")
data class LocalAliasEntity(
    @PrimaryKey
    val targetUserId: String,
    val overriddenDisplayName: String?,
    val overriddenAvatarUri: String?,
    val modifiedTimestamp: Long = System.currentTimeMillis()
)
