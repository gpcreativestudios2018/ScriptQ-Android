package com.gpcreativestudios.scriptq.data

import kotlinx.coroutines.flow.Flow

class ScriptRepository(private val scriptDao: ScriptDao) {

    val allScripts: Flow<List<Script>> = scriptDao.getAllScripts()
    val allScriptsByUpdated: Flow<List<Script>> = scriptDao.getAllScriptsByUpdated()
    val allScriptsByTitle: Flow<List<Script>> = scriptDao.getAllScriptsByTitle()

    fun searchScriptsCreated(query: String) = scriptDao.searchScriptsCreated(query)
    fun searchScriptsUpdated(query: String) = scriptDao.searchScriptsUpdated(query)
    fun searchScriptsTitle(query: String) = scriptDao.searchScriptsTitle(query)

    suspend fun insert(script: Script) {
        scriptDao.insert(script)
    }

    suspend fun delete(script: Script) {
        scriptDao.delete(script)
    }
}
