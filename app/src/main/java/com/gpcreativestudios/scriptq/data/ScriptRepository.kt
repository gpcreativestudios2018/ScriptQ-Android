package com.gpcreativestudios.scriptq.data

import kotlinx.coroutines.flow.Flow

class ScriptRepository(private val scriptDao: ScriptDao) {

    val allScripts: Flow<List<Script>> = scriptDao.getAllScripts()

    suspend fun insert(script: Script) {
        scriptDao.insert(script)
    }

    suspend fun delete(script: Script) {
        scriptDao.delete(script)
    }
}
