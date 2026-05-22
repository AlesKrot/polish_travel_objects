package com.aleskrot.zabytki.presentation.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.DpSize
import com.aleskrot.zabytki.BuildKonfig
import com.aleskrot.zabytki.data.repository.HeritageRemoteRepository
import com.aleskrot.zabytki.data.repository.createHttpClient
import com.aleskrot.zabytki.presentation.components.AddHeritageContent
import com.aleskrot.zabytki.presentation.components.AddHeritageDialog
import com.aleskrot.zabytki.presentation.components.ErrorOverlay
import com.aleskrot.zabytki.presentation.components.HeritageInfoPopup
import com.aleskrot.zabytki.presentation.map.components.MapControls
import com.aleskrot.zabytki.presentation.map.components.MapLegend
import com.aleskrot.zabytki.presentation.map.components.MapSearchBar
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.*
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position
import kotlinx.serialization.json.*
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
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
        zoom = 10.0
    )
    val camera = rememberCameraState(firstPosition = initialPosition)

    var baseStyleJson by remember { mutableStateOf<String?>(null) }
    var desktopStyleJson by remember { mutableStateOf<String?>(null) }

    // GeoJSON string generation
    val geoJsonString = remember(items) {
        if (items.isEmpty()) {
            println("JVM Map: No items to display yet")
            return@remember null
        }
        println("JVM Map: Processing ${items.size} items for GeoJSON")
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

    LaunchedEffect(mapTilerKey, styleId) {
        if (mapTilerKey.isNotEmpty()) {
            try {
                baseStyleJson = httpClient.get(styleUrl).bodyAsText()
                println("JVM Map: Base style loaded successfully")
            } catch (e: Exception) { 
                println("JVM Map: Error loading base style: ${e.message}")
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
                println("JVM Map: Desktop style updated with markers")
            } catch (e: Exception) { 
                println("JVM Map: Error injecting GeoJSON: ${e.message}")
            }
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
            onMapLongClick = { clickPos, _ ->
                println("JVM Map: Long click at ${clickPos.latitude}, ${clickPos.longitude}")
                focusManager.clearFocus()
                viewModel.onMapLongClick(clickPos)
                ClickResult.Consume
            },
            onMapClick = { clickPos, offset ->
                println("JVM Map: Click at ${clickPos.latitude}, ${clickPos.longitude}")
                focusManager.clearFocus()
                val projection = camera.projection
                val clickX = offset.x.value
                val clickY = offset.y.value
                val threshold = 40.0 
                
                val pointsNear = items.filter { item ->
                    val pos = item.getPosition() ?: return@filter false
                    if (projection != null) {
                        val screenLoc = projection.screenLocationFromPosition(pos)
                        val dx = screenLoc.x.value - clickX
                        val dy = screenLoc.y.value - clickY
                        dx * dx + dy * dy < threshold * threshold
                    } else {
                        false
                    }
                }
                
                if (pointsNear.isNotEmpty()) {
                    if (pointsNear.size > 1) {
                        val nextZoom = (camera.position.zoom + 3.0).coerceAtMost(18.0)
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
        ) { }

        // --- SEPARATE WINDOWS FOR EVERYTHING ---

        // 1. Search Bar Window
        Window(
            onCloseRequest = {},
            state = rememberWindowState(position = WindowPosition(Alignment.TopCenter), size = DpSize(450.dp, 100.dp)),
            title = "Search",
            undecorated = true,
            alwaysOnTop = true,
            transparent = true
        ) {
            MaterialTheme {
                Surface(modifier = Modifier.padding(8.dp), shape = RoundedCornerShape(12.dp), shadowElevation = 8.dp) {
                    MapSearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }
            }
        }

        // 2. Map Legend Window
        Window(
            onCloseRequest = {},
            state = rememberWindowState(position = WindowPosition(Alignment.BottomStart), size = DpSize(320.dp, 220.dp)),
            title = "Legend",
            undecorated = true,
            alwaysOnTop = true,
            transparent = true
        ) {
            MaterialTheme {
                MapLegend(modifier = Modifier.fillMaxSize().padding(8.dp))
            }
        }

        // 3. Info popup
        selectedItem?.let { item ->
            Window(
                onCloseRequest = { viewModel.onDismissPopup() },
                state = rememberWindowState(size = DpSize(600.dp, 700.dp)),
                title = item.itemLabel,
                alwaysOnTop = true
            ) {
                MaterialTheme {
                    HeritageInfoPopup(
                        item = item,
                        onDismiss = { viewModel.onDismissPopup() },
                        onDelete = { viewModel.deleteItem(item) }
                    )
                }
            }
        }

        // 4. Add object popup
        pendingNewPosition?.let { position ->
            Window(
                onCloseRequest = { viewModel.onDismissAddDialog() },
                state = rememberWindowState(size = DpSize(500.dp, 500.dp)),
                title = "Add new object",
                alwaysOnTop = true
            ) {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        AddHeritageContent(
                            position = position,
                            onAdd = { name, category, imageUrl ->
                                viewModel.addNewItem(name, category, imageUrl)
                            }
                        )
                    }
                }
            }
        }

        error?.let { errorMsg ->
            Window(
                onCloseRequest = { viewModel.loadItems() },
                state = rememberWindowState(size = DpSize(400.dp, 200.dp)),
                title = "Error",
                alwaysOnTop = true
            ) {
                MaterialTheme {
                    ErrorOverlay(message = errorMsg, onRetry = { viewModel.loadItems() })
                }
            }
        }
    }
}
