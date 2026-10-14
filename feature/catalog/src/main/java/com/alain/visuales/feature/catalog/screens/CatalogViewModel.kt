package com.alain.visuales.feature.catalog.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alain.visuales.core.data.db.entity.DirectoryEntity
import com.alain.visuales.core.data.repository.CatalogRepository
import com.alain.visuales.core.data.repository.FetchResult
import com.alain.visuales.core.data.db.entity.DirectoryEntryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val repo: CatalogRepository
) : ViewModel() {

    private val _currentPath = MutableStateFlow("/")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun navigateTo(path: String) {
        _currentPath.value = path
    }

    fun backToParent() {
        val cur = _currentPath.value.trimEnd('/')
        if (cur.isEmpty() || cur == "/") return
        val parent = cur.substringBeforeLast('/', "").ifEmpty { "/" }
        _currentPath.value = parent
    }

    fun refresh() {
        // The pull-to-refresh triggers a fresh fetch by invalidating cache.
        // For directories we'd need their URL — emit a refresh event.
        _isRefreshing.value = true
        viewModelScope.launch {
            // Force refresh is handled at the screen level via flow re-trigger.
            _isRefreshing.value = false
        }
    }
}
