package com.aleskrot.zabytki.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aleskrot.zabytki.domain.model.HeritageItem
import com.aleskrot.zabytki.domain.repository.HeritageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.maplibre.spatialk.geojson.Position
import kotlinx.datetime.Clock

class MapViewModel(
    private val repository: HeritageRepository
) : ViewModel() {

    private val _allItems = MutableStateFlow<List<HeritageItem>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val items: StateFlow<List<HeritageItem>> = combine(_allItems, _searchQuery, _selectedCategory) { items, query, category ->
        items.filter { item ->
            val matchesQuery = query.isEmpty() || item.itemLabel.contains(query, ignoreCase = true)
            val matchesCategory = category == null || item.categoryLabel == category
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedItem = MutableStateFlow<HeritageItem?>(null)
    val selectedItem: StateFlow<HeritageItem?> = _selectedItem.asStateFlow()

    private val _pendingNewPosition = MutableStateFlow<Position?>(null)
    val pendingNewPosition: StateFlow<Position?> = _pendingNewPosition.asStateFlow()

    private var isLoading = false

    init {
        loadItems()
    }

    fun loadItems() {
        if (isLoading || _allItems.value.isNotEmpty()) return
        isLoading = true
        _error.value = null
        viewModelScope.launch {
            try {
                val result = repository.getHeritageItems()
                _allItems.value = result
            } catch (e: Exception) {
                if (_allItems.value.isEmpty()) {
                    _error.value = "Server unreachable"
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun onMarkerClick(item: HeritageItem) {
        _selectedItem.value = item
    }

    fun onDismissPopup() {
        _selectedItem.value = null
    }

    fun onMapLongClick(position: Position) {
        _pendingNewPosition.value = position
    }

    fun onDismissAddDialog() {
        _pendingNewPosition.value = null
    }

    fun addNewItem(name: String, category: String, imageUrl: String) {
        val pos = _pendingNewPosition.value ?: return
        _pendingNewPosition.value = null
        
        val newItem = HeritageItem(
            item = "custom-${Clock.System.now().toEpochMilliseconds()}",
            itemLabel = name,
            categoryLabel = category,
            coords = "Point(${pos.longitude} ${pos.latitude})",
            image = imageUrl
        )

        viewModelScope.launch {
            try {
                repository.addHeritageItem(newItem)
                // Refresh list forcing server request
                val result = repository.getHeritageItems(forceRefresh = true)
                _allItems.value = result
            } catch (e: Exception) {
                _error.value = "Failed to add item: ${e.message}"
            }
        }
    }

    fun deleteItem(item: HeritageItem) {
        viewModelScope.launch {
            try {
                repository.deleteHeritageItem(item.item)
                _selectedItem.value = null
                // Refresh list forcing server request
                val result = repository.getHeritageItems(forceRefresh = true)
                _allItems.value = result
            } catch (e: Exception) {
                _error.value = "Failed to delete item: ${e.message}"
            }
        }
    }

    fun getCategories(): List<String> {
        return _allItems.value.map { it.categoryLabel }.distinct().sorted()
    }
}
