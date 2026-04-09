package com.gpcreativestudios.scriptq.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY is_favorite DESC, created_at DESC")
    fun getAllScripts(): Flow<List<Script>>

    @Query("SELECT * FROM scripts ORDER BY is_favorite DESC, updated_at DESC")
    fun getAllScriptsByUpdated(): Flow<List<Script>>

    @Query("SELECT * FROM scripts ORDER BY is_favorite DESC, title ASC")
    fun getAllScriptsByTitle(): Flow<List<Script>>

    @Query("SELECT * FROM scripts WHERE title LIKE '%' || :searchQuery || '%' OR text_content LIKE '%' || :searchQuery || '%' ORDER BY is_favorite DESC, created_at DESC")
    fun searchScriptsCreated(searchQuery: String): Flow<List<Script>>

    @Query("SELECT * FROM scripts WHERE title LIKE '%' || :searchQuery || '%' OR text_content LIKE '%' || :searchQuery || '%' ORDER BY is_favorite DESC, updated_at DESC")
    fun searchScriptsUpdated(searchQuery: String): Flow<List<Script>>

    @Query("SELECT * FROM scripts WHERE title LIKE '%' || :searchQuery || '%' OR text_content LIKE '%' || :searchQuery || '%' ORDER BY is_favorite DESC, title ASC")
    fun searchScriptsTitle(searchQuery: String): Flow<List<Script>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(script: Script)

    @Delete
    suspend fun delete(script: Script)

    @Query("DELETE FROM scripts")
    suspend fun deleteAll()
}
