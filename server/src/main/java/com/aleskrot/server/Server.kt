package com.aleskrot.server

import com.aleskrot.server.repository.HeritageRepository
import com.aleskrot.server.routes.heritageRoutes
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*

fun main() {
    val repository = HeritageRepository()

    AppConfig.initDatabase(repository)
    
    embeddedServer(Netty, port = AppConfig.PORT) {
        configurePlugins()
        
        routing {
            heritageRoutes(repository)
        }
    }.start(wait = true)
}
