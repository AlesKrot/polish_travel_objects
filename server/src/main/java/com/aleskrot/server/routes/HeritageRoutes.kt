package com.aleskrot.server.routes

import com.aleskrot.server.models.HeritageItem
import com.aleskrot.server.repository.HeritageRepository
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.HttpStatusCode

fun Route.heritageRoutes(repository: HeritageRepository) {
    route("/heritage") {
        get {
            call.respond(repository.getAllItems())
        }
        post {
            try {
                val item = call.receive<HeritageItem>()
                repository.addItem(item)
                call.respond(HttpStatusCode.Created, item)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid data: ${e.message}")
            }
        }
    }
}