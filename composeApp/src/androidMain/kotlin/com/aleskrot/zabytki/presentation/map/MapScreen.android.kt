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
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.aleskrot.zabytki.BuildKonfig
import com.aleskrot.zabytki.getPlatform
import com.aleskrot.zabytki.data.repository.HeritageRemoteRepository
import com.aleskrot.zabytki.data.repository.createHttpClient
import com.aleskrot.zabytki.domain.model.HeritageItem
import com.aleskrot.zabytki.presentation.components.ErrorOverlay
import com.aleskrot.zabytki.presentation.components.HeritageInfoPopup
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.*
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position
import kotlinx.serialization.json.*
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.maplibre.compose.expressions.dsl.*

@Composable
actual fun MapScreen() {
    val httpClient = remember { createHttpClient() }
    val repository = remember { HeritageRemoteRepository(httpClient) }
    val viewModel = remember { MapViewModel(repository) }
    
    val items by viewModel.items.collectAsState()
    val error by viewModel.error.collectAsState()
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

    // 1. GeoJSON string for items
    val geoJsonString = remember(items) {
        if (items.isEmpty()) return@remember null
        val features = items.mapNotNull { item ->
            val pos = item.getPosition() ?: return@mapNotNull null
            buildJsonObject {
                put("type", "Feature")
                put("geometry", buildJsonObject {
                    put("type", "Point")
                    put("coordinates", buildJsonArray { add(pos.longitude); add(pos.latitude) })
                })
                put("properties", buildJsonObject {
                    put("id", item.item)
                    put("label", item.itemLabel)
                })
                put("id", item.item)
            }
        }
        buildJsonObject {
            put("type", "FeatureCollection")
            put("features", JsonArray(features))
        }.toString()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            cameraState = camera,
            baseStyle = BaseStyle.Uri(styleUrl),
            onMapClick = { _, offset ->
                val projection = camera.projection ?: return@MaplibreMap ClickResult.Pass
                val clickX = offset.x.value
                val clickY = offset.y.value
                val threshold = 25.0
                val pointsNear = items.filter { item ->
                    val pos = item.getPosition() ?: return@filter false
                    val screenLoc = projection.screenLocationFromPosition(pos)
                    val dx = screenLoc.x.value - clickX
                    val dy = screenLoc.y.value - clickY
                    dx * dx + dy * dy < threshold * threshold
                }
                if (pointsNear.isNotEmpty()) {
                    pointsNear.minByOrNull { item ->
                        val pos = item.getPosition()!!
                        val screenLoc = projection.screenLocationFromPosition(pos)
                        val dx = screenLoc.x.value - clickX
                        val dy = screenLoc.y.value - clickY
                        dx * dx + dy * dy
                    }?.let { viewModel.onMarkerClick(it) }
                    ClickResult.Consume
                } else ClickResult.Pass
            }
        ) {
            geoJsonString?.let { json ->
                key(json) {
                    val source = rememberGeoJsonSource(
                        data = GeoJsonData.JsonString(json),
                        options = GeoJsonOptions(cluster = true, clusterMaxZoom = 14, clusterRadius = 50)
                    )
                    CircleLayer(
                        id = "heritage-clusters",
                        source = source,
                        filter = feature.has("point_count"),
                        color = const(Color(0xFF51BBD6)),
                        radius = const(20.dp)
                    )
                    SymbolLayer(
                        id = "heritage-cluster-count",
                        source = source,
                        filter = feature.has("point_count"),
                        textField = format(span(feature["point_count_abbreviated"].convertToString())),
                        textColor = const(Color.White)
                    )
                    CircleLayer(
                        id = "heritage-points",
                        source = source,
                        filter = !feature.has("point_count"),
                        color = const(Color.Red),
                        radius = const(10.dp)
                    )
                }
            }
        }

        selectedItem?.let { item ->
            HeritageInfoPopup(item = item, onDismiss = { viewModel.onDismissPopup() })
        }

        error?.let { errorMsg ->
            ErrorOverlay(
                message = errorMsg,
                onRetry = { viewModel.loadItems() }
            )
        }
    }
}
