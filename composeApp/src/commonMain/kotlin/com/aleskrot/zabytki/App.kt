package com.aleskrot.zabytki

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            MapScreen()
        }
    }
}

@Composable
fun MapScreen() {
    val variant = if (isSystemInDarkTheme()) "dark" else "light"
    val mapTilerKey = BuildKonfig.MAPTILER_KEY
    val styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=$mapTilerKey"
    val camera = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(latitude = 52.2297, longitude = 21.0122),
            zoom = 10.0
        )
    )

    MaplibreMap(
        modifier = Modifier.fillMaxSize(),
        cameraState = camera,
        options = MapOptions(
            gestureOptions = GestureOptions.Standard,
            ornamentOptions = OrnamentOptions.AllEnabled
        ),
        baseStyle = BaseStyle.Uri(styleUrl)
    )
}
