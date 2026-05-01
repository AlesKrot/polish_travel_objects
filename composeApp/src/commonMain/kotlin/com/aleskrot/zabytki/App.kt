package com.aleskrot.zabytki

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aleskrot.zabytki.presentation.map.MapScreen

@Composable
fun App() {
    println("App: Composition started")
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            println("App: Navigating to MapScreen")
            MapScreen()
        }
    }
}
