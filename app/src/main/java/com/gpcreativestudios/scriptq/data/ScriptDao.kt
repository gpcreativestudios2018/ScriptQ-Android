package com.gpcreativestudios.scriptq.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY id DESC")
    fun getAllScripts(): Flow<List<Script>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(script: Script)

    @Delete
    suspend fun delete(script: Script)

    @Query("DELETE FROM scripts")
    suspend fun deleteAll()
}
