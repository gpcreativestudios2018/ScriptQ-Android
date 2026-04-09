package com.gpcreativestudios.scriptq.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.gpcreativestudios.scriptq.data.Script
import com.gpcreativestudios.scriptq.data.ScriptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.combine

enum class SortOrder {
    CREATED, UPDATED, TITLE
}

class ScriptViewModel(private val repository: ScriptRepository) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortOrder.CREATED)
    private val _searchQuery = MutableStateFlow("")
    
    val allScripts: LiveData<List<Script>> = combine(_sortOrder, _searchQuery) { order, query ->
        Pair(order, query)
    }.flatMapLatest { (order, query) ->
        if (query.isBlank()) {
            when (order) {
                SortOrder.CREATED -> repository.allScripts
                SortOrder.UPDATED -> repository.allScriptsByUpdated
                SortOrder.TITLE -> repository.allScriptsByTitle
            }
        } else {
            when (order) {
                SortOrder.CREATED -> repository.searchScriptsCreated(query)
                SortOrder.UPDATED -> repository.searchScriptsUpdated(query)
                SortOrder.TITLE -> repository.searchScriptsTitle(query)
            }
        }
    }.asLiveData()

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

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
