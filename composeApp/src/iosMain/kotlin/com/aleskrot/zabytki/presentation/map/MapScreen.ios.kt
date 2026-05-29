package com.aleskrot.zabytki.presentation.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.aleskrot.zabytki.BuildKonfig
import com.aleskrot.zabytki.data.repository.HeritageRemoteRepository
import com.aleskrot.zabytki.data.repository.createHttpClient
import com.aleskrot.zabytki.presentation.components.AddHeritageDialog
import com.aleskrot.zabytki.presentation.components.ErrorOverlay
import com.aleskrot.zabytki.presentation.components.HeritageInfoPopup
import com.aleskrot.zabytki.presentation.map.components.MapControls
import com.aleskrot.zabytki.presentation.map.components.MapSearchBar
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import com.aleskrot.zabytki.presentation.theme.AppRed
import org.maplibre.compose.map.*
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position
import kotlinx.serialization.json.*
import org.maplibre.compose.expressions.dsl.*
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch

@Composable
actual fun MapScreen() {
    val httpClient = remember { createHttpClient() }
    val repository = remember { HeritageRemoteRepository(httpClient) }
    val viewModel = remember { MapViewModel(repository) }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    
    val items by viewModel.items.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()
    val pendingNewPosition by viewModel.pendingNewPosition.collectAsState()

    val variant = if (isSystemInDarkTheme()) "dark" else "light"
    val mapTilerKey = BuildKonfig.MAPTILER_KEY
    val styleId = if (variant == "dark") "darkmatter" else "streets-v2"
    val styleUrl = "https://api.maptiler.com/maps/$styleId/style.json?key=$mapTilerKey"
    
    val initialPosition = CameraPosition(
        target = Position(longitude = 21.0122, latitude = 52.2297),
        zoom = 10.0,
    )
    val camera = rememberCameraState(firstPosition = initialPosition)

    // GeoJSON string for items
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
            onMapLongClick = { clickPos, _ ->
                focusManager.clearFocus()
                viewModel.onMapLongClick(clickPos)
                ClickResult.Consume
            },
            onMapClick = { clickPos, offset ->
                focusManager.clearFocus()
                val projection = camera.projection ?: return@MaplibreMap ClickResult.Pass
                val clickX = offset.x.value
                val clickY = offset.y.value
                val threshold = 25.0
                
                val pointsNear = items.filter { item ->
                    val pos = item.getPosition() ?: return@filter false
                    val screenLoc = projection.screenLocationFromPosition(pos)
                    val dx = screenLoc.x.value - clickX
                    val dy = screenLoc.y.value - clickY
                    (dx * dx + dy * dy) < (threshold * threshold)
                }
                
                if (pointsNear.isNotEmpty()) {
                    val firstPos = pointsNear.first().getPosition()
                    val allSameLocation = pointsNear.all { it.getPosition() == firstPos }
                    
                    if (pointsNear.size > 1 && !allSameLocation) {
                        val currentZoom = camera.position.zoom
                        val nextZoom = (currentZoom + 2.5).coerceAtMost(18.0)
                        coroutineScope.launch {
                            camera.animateTo(
                                CameraPosition(target = clickPos, zoom = nextZoom),
                                duration = 500.milliseconds
                            )
                        }
                    } else {
                        viewModel.onMarkerClick(pointsNear.first())
                    }
                    ClickResult.Consume
                } else {
                    ClickResult.Pass
                }
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
                        color = const(AppRed),
                        radius = const(10.dp)
                    )
                }
            }
        }

        // UI Overlay
        MapSearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        )

        MapControls(
            modifier = Modifier.align(Alignment.CenterEnd).padding(bottom = 32.dp),
            onZoomIn = {
                coroutineScope.launch {
                    camera.animateTo(
                        camera.position.copy(zoom = (camera.position.zoom + 1.0).coerceAtMost(20.0)),
                        duration = 300.milliseconds
                    )
                }
            },
            onZoomOut = {
                coroutineScope.launch {
                    camera.animateTo(
                        camera.position.copy(zoom = (camera.position.zoom - 1.0).coerceAtLeast(1.0)),
                        duration = 300.milliseconds
                    )
                }
            }
        ) {
            coroutineScope.launch {
                camera.animateTo(initialPosition, duration = 500.milliseconds)
            }
        }

        selectedItem?.let { item ->
            HeritageInfoPopup(
                item = item,
                onDismiss = { viewModel.onDismissPopup() },
                onDelete = { viewModel.deleteItem(item) }
            )
        }

        pendingNewPosition?.let { position ->
            AddHeritageDialog(
                position = position,
                onDismiss = { viewModel.onDismissAddDialog() },
                onAdd = { name, category, imageUrl ->
                    viewModel.addNewItem(name, category, imageUrl)
                }
            )
        }

        error?.let { errorMsg ->
            ErrorOverlay(
                message = errorMsg,
                onRetry = { viewModel.loadItems() }
            )
        }
    }
}
