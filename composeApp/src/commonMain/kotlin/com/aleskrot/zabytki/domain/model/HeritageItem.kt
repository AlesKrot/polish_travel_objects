package com.aleskrot.zabytki.domain.model

import kotlinx.serialization.Serializable
import org.maplibre.spatialk.geojson.Position

@Serializable
data class HeritageItem(
    val item: String = "",          // URL Wiki
    val itemLabel: String = "",     // Name (ex. "Nawer")
    val coords: String = "",        // Coordinates "Point(21.15 52.21)"
    val categoryLabel: String = "", // Category (ex. "dzielnica miasta")
    val image: String = ""          // URL image
) {
    /**
     * Returns the image URL using HTTPS if it's currently HTTP.
     */
    fun getImageUrl(): String {
        return if (image.startsWith("http://")) {
            image.replaceFirst("http://", "https://")
        } else {
            image
        }
    }
    /**
     * Parses the "Point(lon lat)" string into a Position object.
     */
    fun getPosition(): Position? {
        if (coords.isEmpty()) return null
        return try {
            val content = coords.substringAfter("Point(").substringBefore(")")
            val parts = content.trim().split(Regex("\\s+"))
            if (parts.size >= 2) {
                val longitude = parts[0].toDoubleOrNull() ?: return null
                val latitude = parts[1].toDoubleOrNull() ?: return null
                Position(longitude = longitude, latitude = latitude)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
