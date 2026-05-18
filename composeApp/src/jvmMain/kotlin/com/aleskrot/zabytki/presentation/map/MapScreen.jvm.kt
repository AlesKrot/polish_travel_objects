package com.aleskrot.zabytki.presentation.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aleskrot.zabytki.BuildKonfig
import com.aleskrot.zabytki.data.repository.HeritageRemoteRepository
import com.aleskrot.zabytki.data.repository.createHttpClient
import com.aleskrot.zabytki.presentation.components.ErrorOverlay
import com.aleskrot.zabytki.presentation.components.HeritageInfoPopup
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.*
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position
import kotlinx.serialization.json.*
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState

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

    var baseStyleJson by remember { mutableStateOf<String?>(null) }
    var desktopStyleJson by remember { mutableStateOf<String?>(null) }

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

    // 2. Fetch and prepare style with injection
    LaunchedEffect(mapTilerKey, styleId) {
        if (mapTilerKey.isNotEmpty()) {
            try {
                baseStyleJson = httpClient.get(styleUrl).bodyAsText()
            } catch (e: Exception) {
                println("Error fetching style: ${e.message}")
            }
        }
    }

    LaunchedEffect(geoJsonString, baseStyleJson) {
        if (baseStyleJson != null && geoJsonString != null) {
            try {
                val json = Json { ignoreUnknownKeys = true }
                val styleObj = json.parseToJsonElement(baseStyleJson!!).jsonObject.toMutableMap()
                val sources = styleObj["sources"]?.jsonObject?.toMutableMap() ?: mutableMapOf()
                
                sources["heritage-source"] = buildJsonObject {
                    put("type", "geojson")
                    put("data", json.parseToJsonElement(geoJsonString))
                    put("cluster", true)
                    put("clusterMaxZoom", 14)
                    put("clusterRadius", 50)
                }
                styleObj["sources"] = JsonObject(sources)
                
                val layers = styleObj["layers"]?.jsonArray?.toMutableList() ?: mutableListOf()
                
                // Add layers manually for desktop style
                layers.add(buildJsonObject {
                    put("id", "heritage-clusters")
                    put("type", "circle")
                    put("source", "heritage-source")
                    put("filter", buildJsonArray { add("has"); add("point_count") })
                    putJsonObject("paint") {
                        put("circle-color", "#51bbd6")
                        put("circle-radius", buildJsonArray {
                            add("step"); add(buildJsonArray { add("get"); add("point_count") })
                            add(20.0); add(100.0); add(30.0); add(750.0); add(40.0)
                        })
                    }
                })
                layers.add(buildJsonObject {
                    put("id", "heritage-cluster-count")
                    put("type", "symbol")
                    put("source", "heritage-source")
                    put("filter", buildJsonArray { add("has"); add("point_count") })
                    putJsonObject("layout") {
                        put("text-field", "{point_count_abbreviated}")
                        put("text-size", 12)
                    }
                    putJsonObject("paint") { put("text-color", "#ffffff") }
                })
                layers.add(buildJsonObject {
                    put("id", "heritage-points")
                    put("type", "circle")
                    put("source", "heritage-source")
                    put("filter", buildJsonArray { add("!"); add(buildJsonArray { add("has"); add("point_count") }) })
                    putJsonObject("paint") {
                        put("circle-color", "#FF0000")
                        put("circle-radius", 8)
                    }
                })
                
                styleObj["layers"] = JsonArray(layers)
                desktopStyleJson = JsonObject(styleObj).toString()
            } catch (e: Exception) { }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val style = if (desktopStyleJson != null) {
            BaseStyle.Json(desktopStyleJson!!)
        } else {
            BaseStyle.Uri(styleUrl)
        }

        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            cameraState = camera,
            baseStyle = style,
            onMapClick = { clickPos, offset ->
                val projection = camera.projection
                val clickX = offset.x.value
                val clickY = offset.y.value
                val threshold = 35.0 
                
                val pointsNear = items.filter { item ->
                    val pos = item.getPosition() ?: return@filter false
                    if (projection != null) {
                        val screenLoc = projection.screenLocationFromPosition(pos)
                        val dx = screenLoc.x.value - clickX
                        val dy = screenLoc.y.value - clickY
                        dx * dx + dy * dy < threshold * threshold
                    } else {
                        val dLon = pos.longitude - clickPos.longitude
                        val dLat = pos.latitude - clickPos.latitude
                        val geoThreshold = 0.1 / (camera.position.zoom + 1.0)
                        dLon * dLon + dLat * dLat < geoThreshold * geoThreshold
                    }
                }
                
                if (pointsNear.isNotEmpty()) {
                    val closest = pointsNear.minByOrNull { item ->
                        val pos = item.getPosition()!!
                        val dLon = pos.longitude - clickPos.longitude
                        val dLat = pos.latitude - clickPos.latitude
                        dLon * dLon + dLat * dLat
                    }
                    closest?.let { 
                        viewModel.onMarkerClick(it) 
                    }
                    ClickResult.Consume
                } else {
                    ClickResult.Pass
                }
            }
        )

        selectedItem?.let { item ->
            DialogWindow(
                onCloseRequest = { viewModel.onDismissPopup() },
                state = rememberDialogState(width = 600.dp, height = 700.dp),
                title = item.itemLabel,
                resizable = false,
                focusable = true
            ) {
                HeritageInfoPopup(item = item, onDismiss = { viewModel.onDismissPopup() })
            }
        }

        error?.let { errorMsg ->
            ErrorOverlay(
                message = errorMsg,
                onRetry = { viewModel.loadItems() }
            )
        }
    }
}
