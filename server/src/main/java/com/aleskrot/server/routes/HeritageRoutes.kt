package com.aleskrot.server.routes

import com.aleskrot.server.models.HeritageItem
import com.aleskrot.server.repository.HeritageRepository
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.HttpStatusCode

fun Route.heritageRoutes(repository: HeritageRepository) {
    get("/heritage") {
        call.respond(repository.getAllItems())
    }
    post("/heritage") {
        try {
            val item = call.receive<HeritageItem>()
            repository.addItem(item)
            call.respond(HttpStatusCode.Created, item)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, "Invalid data: ${e.message}")
        }
    }
    delete("/heritage") {
        val id = call.request.queryParameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
        repository.deleteItem(id)
        call.respond(HttpStatusCode.NoContent)
    }
}