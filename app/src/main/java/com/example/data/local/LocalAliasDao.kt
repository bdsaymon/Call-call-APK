package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalAliasDao {
    @Query("SELECT * FROM local_aliases ORDER BY modifiedTimestamp DESC")
    fun observeAllAliases(): Flow<List<LocalAliasEntity>>

    @Query("SELECT * FROM local_aliases WHERE targetUserId = :targetUserId LIMIT 1")
    fun observeAliasForUser(targetUserId: String): Flow<LocalAliasEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlias(alias: LocalAliasEntity)

    @Query("DELETE FROM local_aliases WHERE targetUserId = :targetUserId")
    suspend fun deleteAliasForUser(targetUserId: String)
}
