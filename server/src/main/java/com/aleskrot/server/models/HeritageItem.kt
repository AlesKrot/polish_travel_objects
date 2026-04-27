package com.aleskrot.server.models

import kotlinx.serialization.Serializable

@Serializable
data class HeritageItem(
    val item: String,          // URL Wiki
    val itemLabel: String,     // Name (ex. "Nawer")
    val coords: String,        // Coordinates "Point(21.15 52.21)"
    val categoryLabel: String, // Category (ex. "dzielnica miasta")
    val image: String          // URL image
)