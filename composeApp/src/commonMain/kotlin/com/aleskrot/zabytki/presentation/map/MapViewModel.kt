package com.aleskrot.zabytki.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aleskrot.zabytki.domain.model.HeritageItem
import com.aleskrot.zabytki.domain.repository.HeritageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    private val repository: HeritageRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<HeritageItem>>(emptyList())
    val items: StateFlow<List<HeritageItem>> = _items.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedItem = MutableStateFlow<HeritageItem?>(null)
    val selectedItem: StateFlow<HeritageItem?> = _selectedItem.asStateFlow()

    private var isLoading = false

    init {
        println("MapViewModel: init block")
        loadItems()
    }

    fun loadItems() {
        if (isLoading) return
        isLoading = true
        _error.value = null
        viewModelScope.launch {
            println("MapViewModel: Starting loadItems...")
            try {
                val result = repository.getHeritageItems()
                println("MapViewModel: Loaded ${result.size} items")
                _items.value = result
                if (result.isEmpty()) {
                    // Optionally set error if empty list means failure in your context
                }
            } catch (e: Exception) {
                println("MapViewModel: Error: ${e.message}")
                _error.value = "Server unreachable"
            } finally {
                isLoading = false
            }
        }
    }

    fun onMarkerClick(item: HeritageItem) {
        _selectedItem.value = item
    }

    fun onDismissPopup() {
        _selectedItem.value = null
    }
}
