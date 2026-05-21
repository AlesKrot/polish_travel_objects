package com.aleskrot.server

import com.aleskrot.server.database.HeritageTable
import com.aleskrot.server.models.HeritageItem
import com.aleskrot.server.repository.HeritageRepository
import com.aleskrot.server.routes.heritageRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.http.content.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.json.Json

fun main() {
    // Ініцыялізацыя БД (можна вынесці ў Config)
    Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")

    transaction {
        SchemaUtils.create(HeritageTable)
    }

    val repository = HeritageRepository()

    seedDatabase(repository)
    
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()
        }
        install(CachingHeaders) {
            options { _, _ ->
                CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 3600))
            }
        }
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.AccessControlAllowOrigin)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Options)
        }
        routing {
            heritageRoutes(repository)
        }
    }.start(wait = true)
}

private fun seedDatabase(repository: HeritageRepository) {
    runBlocking {
        if (repository.getAllItems().isEmpty()) {
            val jsonText = object {}.javaClass.classLoader
                .getResource("heritage.json")?.readText() ?: "[]"

            val json = Json { ignoreUnknownKeys = true }
            val items = json.decodeFromString<List<HeritageItem>>(jsonText)

            for (item in items) {
                repository.addItem(item)
            }
            println("Added ${items.size} objects into base.")
        }
    }
}