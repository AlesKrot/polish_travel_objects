package com.aleskrot.zabytki.domain.model

data class Game(
    val id: String,
    val title: String,
    val gogUrl: String,
    val completed: Boolean = false
)