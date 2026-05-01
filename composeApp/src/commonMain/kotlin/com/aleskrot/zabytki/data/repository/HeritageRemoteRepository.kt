package com.aleskrot.zabytki.data.repository

import com.aleskrot.zabytki.BuildKonfig
import com.aleskrot.zabytki.getPlatform
import com.aleskrot.zabytki.domain.model.HeritageItem
import com.aleskrot.zabytki.domain.repository.HeritageRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class HeritageRemoteRepository(
    private val httpClient: HttpClient
) : HeritageRepository {

    private val baseUrl: String by lazy {
        val rawUrl = BuildKonfig.BASE_URL
        if (rawUrl.contains("localhost") || rawUrl.contains("127.0.0.1")) {
            val platform = getPlatform().name.lowercase()
            if (platform.contains("android")) {
                rawUrl.replace("localhost", "10.0.2.2").replace("127.0.0.1", "10.0.2.2")
            } else {
                rawUrl
            }
        } else {
            rawUrl
        }
    }

    override suspend fun getHeritageItems(): List<HeritageItem> {
        return try {
            val url = "$baseUrl/heritage"
            println("HeritageRemoteRepository: Fetching from $url")
            
            val response = httpClient.get(url)
            println("HeritageRemoteRepository: Response status: ${response.status}")
            
            val items: List<HeritageItem> = response.body()
            println("HeritageRemoteRepository: Successfully fetched ${items.size} items")
            
            items
        } catch (e: Exception) {
            val errorMsg = "Error fetching items: ${e.message}"
            println("HeritageRemoteRepository: $errorMsg")
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getHeritageItemById(id: String): HeritageItem? {
        // У ідэале гэта павінен быць асобны запыт да API, напрыклад /heritage/{id}
        return getHeritageItems().find { it.item == id }
    }

    override suspend fun searchHeritageItems(query: String): List<HeritageItem> {
        return getHeritageItems().filter { 
            it.itemLabel.contains(query, ignoreCase = true) || 
            it.categoryLabel.contains(query, ignoreCase = true)
        }
    }
}

fun createHttpClient(): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                useAlternativeNames = false
            })
        }
    }
}
