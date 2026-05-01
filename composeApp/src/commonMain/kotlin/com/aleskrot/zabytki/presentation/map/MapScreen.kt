package com.aleskrot.zabytki.presentation.map

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aleskrot.zabytki.BuildKonfig
import com.aleskrot.zabytki.getPlatform
import com.aleskrot.zabytki.data.repository.HeritageRemoteRepository
import com.aleskrot.zabytki.data.repository.createHttpClient
import com.aleskrot.zabytki.domain.model.HeritageItem
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.*
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position

@Composable
fun MapScreen() {
    println("MapScreen: Start")
    val isWeb = remember { getPlatform().name.contains("Web") }
    val httpClient = remember { createHttpClient() }
    val repository = remember { HeritageRemoteRepository(httpClient) }
    val viewModel = remember { MapViewModel(repository) }
    
    val items by viewModel.items.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()

    val variant = if (isSystemInDarkTheme()) "dark" else "light"
    val mapTilerKey = BuildKonfig.MAPTILER_KEY
    val styleId = if (variant == "dark") "darkmatter" else "streets-v2"
    val styleUrl = "https://api.maptiler.com/maps/$styleId/style.json?key=$mapTilerKey"
    
    val camera = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(longitude = 21.0122, latitude = 52.2297),
            zoom = 10.0
        )
    )

    val geoJsonData = remember(items) {
        if (items.isEmpty()) return@remember GeoJsonData.JsonString("""{"type":"FeatureCollection","features":[]}""")
        
        val featuresJson = items.joinToString(",") { item ->
            val pos = item.getPosition()
            """
            {
                "type": "Feature",
                "geometry": {
                    "type": "Point",
                    "coordinates": [${pos?.longitude ?: 0.0}, ${pos?.latitude ?: 0.0}]
                },
                "properties": {
                    "id": "${item.item}",
                    "label": "${item.itemLabel.replace("\"", "'")}"
                }
            }
            """.trimIndent()
        }
        
        GeoJsonData.JsonString("""{"type":"FeatureCollection","features":[$featuresJson]}""")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (mapTilerKey.isEmpty()) {
            Text("Error: MAPTILER_KEY is missing", color = Color.Red, modifier = Modifier.align(Alignment.Center))
        } else {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                cameraState = camera,
                baseStyle = BaseStyle.Uri(styleUrl)
            ) {
                if (!isWeb) {
                    val source = rememberGeoJsonSource(data = geoJsonData)
                    CircleLayer(
                        id = "heritage-points",
                        source = source,
                        color = org.maplibre.compose.expressions.dsl.const(Color.Red),
                        radius = org.maplibre.compose.expressions.dsl.const(10.dp),
                        onClick = { features ->
                            val clickedId = features.firstOrNull()?.properties?.get("id")?.toString()
                            val item = items.find { it.item == clickedId }
                            if (item != null) {
                                viewModel.onMarkerClick(item)
                                ClickResult.Consume
                            } else {
                                ClickResult.Pass
                            }
                        }
                    )
                }
            }
        }

        // Дыягнастычная панэль
        Card(
            modifier = Modifier.padding(16.dp).align(Alignment.TopStart),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Items from server: ${items.size}", color = if (items.isEmpty()) Color.Red else Color.Green, style = MaterialTheme.typography.labelSmall)
                if (items.isNotEmpty()) {
                    Text("First: ${items.first().itemLabel}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                if (isWeb) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Web target: Markers disabled (GeoJSON not yet supported)", color = Color.Yellow, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        selectedItem?.let { item ->
            HeritageInfoPopup(
                item = item,
                onDismiss = { viewModel.onDismissPopup() }
            )
        }
    }
}

@Composable
fun HeritageInfoPopup(
    item: HeritageItem,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.itemLabel,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.categoryLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                if (item.image.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Image URL: ${item.image}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Blue
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Закрыць")
                }
            }
        }
    }
}
