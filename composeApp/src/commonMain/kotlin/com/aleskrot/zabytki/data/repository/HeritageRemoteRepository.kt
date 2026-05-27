package com.aleskrot.zabytki.data.repository

import com.aleskrot.zabytki.BuildKonfig
import com.aleskrot.zabytki.getPlatform
import com.aleskrot.zabytki.domain.model.HeritageItem
import com.aleskrot.zabytki.domain.repository.HeritageRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    override suspend fun getHeritageItems(forceRefresh: Boolean): List<HeritageItem> {
        return try {
            val url = "$baseUrl/heritage"
            println("HeritageRemoteRepository: Fetching from $url (forceRefresh=$forceRefresh)")
            
            val response = httpClient.get(url) {
                if (forceRefresh) {
                    header(io.ktor.http.HttpHeaders.CacheControl, "no-cache")
                    header(io.ktor.http.HttpHeaders.Pragma, "no-cache")
                    header(io.ktor.http.HttpHeaders.Expires, "0")
                }
            }
            println("HeritageRemoteRepository: Response status: ${response.status}")
            
            val items: List<HeritageItem> = withContext(Dispatchers.Default) {
                val fetchedItems: List<HeritageItem> = response.body()
                fetchedItems.map { item ->
                    if (item.image.contains("/wiki/Special:FilePath/")) {
                        val fileName = item.image.substringAfter("/wiki/Special:FilePath/")
                        // Direct thumbnail URL is much more stable for Coil
                        val directUrl = "https://commons.wikimedia.org/w/thumb.php?f=$fileName&w=800"
                        item.copy(image = directUrl)
                    } else if (item.image.startsWith("http://")) {
                        item.copy(image = item.image.replaceFirst("http://", "https://"))
                    } else {
                        item
                    }
                }
            }
            println("HeritageRemoteRepository: Successfully fetched ${items.size} items")
            
            items
        } catch (e: Exception) {
            val errorMsg = "Error fetching items: ${e.message}"
            println("HeritageRemoteRepository: $errorMsg")
            e.printStackTrace()
            throw e
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

    override suspend fun addHeritageItem(item: HeritageItem) {
        withContext(Dispatchers.Default) {
            val response = httpClient.post("$baseUrl/heritage") {
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(item)
            }
            if (response.status.value !in 200..299) {
                throw Exception("Failed to add item: ${response.status}")
            }
        }
    }

    override suspend fun deleteHeritageItem(id: String) {
        withContext(Dispatchers.Default) {
            val cleanBaseUrl = baseUrl.removeSuffix("/")
            val response = httpClient.delete("$cleanBaseUrl/heritage") {
                url {
                    parameters.append("id", id)
                }
            }
            if (response.status.value !in 200..299) {
                throw Exception("Failed to delete item: ${response.status}")
            }
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
        install(HttpCache)
    }
}
