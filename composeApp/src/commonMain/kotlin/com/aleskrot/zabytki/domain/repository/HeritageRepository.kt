package com.aleskrot.zabytki.domain.repository

import com.aleskrot.zabytki.domain.model.HeritageItem

interface HeritageRepository {
    suspend fun getHeritageItems(forceRefresh: Boolean = false): List<HeritageItem>
    suspend fun getHeritageItemById(id: String): HeritageItem?
    suspend fun searchHeritageItems(query: String): List<HeritageItem>
    suspend fun addHeritageItem(item: HeritageItem)
    suspend fun deleteHeritageItem(id: String)
}
