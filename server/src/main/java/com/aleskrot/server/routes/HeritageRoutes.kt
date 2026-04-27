package com.aleskrot.server.routes

import com.aleskrot.server.models.HeritageItem
import com.aleskrot.server.repository.HeritageRepository
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.heritageRoutes(repository: HeritageRepository) {
    route("/heritage") {
        get {
            call.respond(repository.getAllItems())
        }
        post {
            val item = call.receive<HeritageItem>()
            repository.addItem(item)
            call.respond(item)
        }
    }
}