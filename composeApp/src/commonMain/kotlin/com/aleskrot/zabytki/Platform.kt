package com.aleskrot.zabytki

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform