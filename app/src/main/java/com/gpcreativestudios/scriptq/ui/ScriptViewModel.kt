package com.gpcreativestudios.scriptq.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.gpcreativestudios.scriptq.data.Script
import com.gpcreativestudios.scriptq.data.ScriptRepository
import kotlinx.coroutines.launch

class ScriptViewModel(private val repository: ScriptRepository) : ViewModel() {

    val allScripts: LiveData<List<Script>> = repository.allScripts.asLiveData()

    fun insert(script: Script) = viewModelScope.launch {
        repository.insert(script)
    }

    fun delete(script: Script) = viewModelScope.launch {
        repository.delete(script)
    }
}

class ScriptViewModelFactory(private val repository: ScriptRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScriptViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScriptViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
