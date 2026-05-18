package com.aleskrot.zabytki

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.aleskrot.zabytki.presentation.map.MapScreen
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*

@OptIn(ExperimentalCoilApi::class)
@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(HttpClient {
                    install(DefaultRequest) {
                        header("User-Agent", "Mozilla/5.0 (ComposeApp; aleskrot@example.com)")
                    }
                }))
            }
            .crossfade(true)
            .build()
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            MapScreen()
        }
    }
}
