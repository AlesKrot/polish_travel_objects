package com.aleskrot.zabytki

class WasmJsPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmJsPlatform()
