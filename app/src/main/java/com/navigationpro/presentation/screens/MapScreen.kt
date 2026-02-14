package com.navigationpro.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.navigationpro.presentation.components.ControlDeckContent
import com.navigationpro.presentation.components.IndianGridOverlay
import com.navigationpro.presentation.state.MapUiState
import com.navigationpro.presentation.viewmodel.MapViewModel
import com.navigationpro.domain.service.CompassSensorManager
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Main Map Screen with HUD overlay
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Request permission launcher
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (hasLocationPermission) {
            viewModel.startLocationUpdates()
        }
    }

    // Start location updates when permission is granted
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            viewModel.startLocationUpdates()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Control deck sheet state
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Map View
        OsmMapView(
            uiState = uiState,
            onMapClick = { viewModel.setFollowLocation(false) },
            onZoomChanged = { viewModel.updateZoom(it) }
        )

        // HUD Overlay
        HudOverlay(
            uiState = uiState,
            onFollowClick = { viewModel.toggleFollowLocation() },
            onControlDeckClick = {
                scope.launch {
                    viewModel.openControlDeck()
                    sheetState.show()
                }
            }
        )
    }

    // Control Deck Bottom Sheet
    if (uiState.isControlDeckOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeControlDeck() },
            sheetState = sheetState,
            containerColor = Color(0xFF0f172a),
            contentColor = Color.White
        ) {
            ControlDeckContent(
                uiState = uiState,
                viewModel = viewModel,
                onClose = {
                    scope.launch {
                        sheetState.hide()
                        viewModel.closeControlDeck()
                    }
                }
            )
        }
    }

    // Error snackbar
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            viewModel.clearError()
        }
    }
}

/**
 * OSMDroid MapView wrapped in Compose
 */
@Composable
fun OsmMapView(
    uiState: MapUiState,
    onMapClick: () -> Unit,
    onZoomChanged: (Double) -> Unit
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(getTileSource(uiState.selectedMapLayer))
                setMultiTouchControls(true)
                minZoomLevel = 3.0
                maxZoomLevel = 20.0
                controller.setZoom(uiState.currentZoom)

                val locationOverlay = MyLocationNewOverlay(this).apply {
                    enableMyLocation()
                    enableFollowLocation()
                    setPersonIcon(createLocationArrowBitmap(ctx, uiState.compassHeading))
                }
                overlays.add(locationOverlay)

                val gridOverlay = IndianGridOverlay(ctx).apply {
                    isGridVisible = uiState.isGridOverlayVisible
                }
                overlays.add(gridOverlay)

                uiState.waypoints.forEach { waypoint ->
                    val marker = Marker(this).apply {
                        position = GeoPoint(waypoint.latitude, waypoint.longitude)
                        title = waypoint.name
                        snippet = waypoint.description
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    overlays.add(marker)
                }
            }
        },
        update = { mapView ->
            mapView.setTileSource(getTileSource(uiState.selectedMapLayer))

            uiState.currentLocation?.let { location ->
                val geoPoint = GeoPoint(location.latitude, location.longitude)

                if (uiState.isFollowingLocation) {
                    mapView.controller.animateTo(geoPoint)
                }

                mapView.overlays.filterIsInstance<MyLocationNewOverlay>()
                    .forEach { overlay ->
                        overlay.setPersonIcon(
                            createLocationArrowBitmap(context, uiState.compassHeading)
                        )
                    }
            }

            mapView.overlays.filterIsInstance<IndianGridOverlay>()
                .forEach { it.isGridVisible = uiState.isGridOverlayVisible }

            if (mapView.zoomLevelDouble != uiState.currentZoom) {
                mapView.controller.setZoom(uiState.currentZoom)
            }

            mapView.invalidate()
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Get tile source for selected map layer
 */
private fun getTileSource(layer: MapUiState.MapLayer): XYTileSource {
    return when (layer) {
        MapUiState.MapLayer.TACTICAL_DARK -> XYTileSource(
            "CartoDB Dark",
            0,
            20,
            256,
            ".png",
            arrayOf("a", "b", "c", "d")
        ) { zoom, x, y ->
            val server = arrayOf("a", "b", "c", "d")[(x + y) % 4]
            "https://$server.basemaps.cartocdn.com/dark_all/$zoom/$x/$y.png"
        }

        MapUiState.MapLayer.SATELLITE -> XYTileSource(
            "ArcGIS Satellite",
            0,
            20,
            256,
            ".jpg",
            arrayOf("server")
        ) { zoom, x, y ->
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$zoom/$y/$x"
        }

        MapUiState.MapLayer.OPEN_TOPO -> XYTileSource(
            "OpenTopoMap",
            0,
            17,
            256,
            ".png",
            arrayOf("a", "b", "c")
        ) { zoom, x, y ->
            val server = arrayOf("a", "b", "c")[(x + y) % 3]
            "https://$server.tile.opentopomap.org/$zoom/$x/$y.png"
        }

        MapUiState.MapLayer.STANDARD -> XYTileSource(
            "OpenStreetMap",
            0,
            20,
            256,
            ".png",
            arrayOf("a", "b", "c")
        ) { zoom, x, y ->
            val server = arrayOf("a", "b", "c")[(x + y) % 3]
            "https://$server.tile.openstreetmap.org/$zoom/$x/$y.png"
        }
    }
}

/**
 * Create location arrow bitmap with rotation
 */
private fun createLocationArrowBitmap(context: android.content.Context, rotation: Float): Bitmap {
    val size = 96
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint().apply {
        color = android.graphics.Color.parseColor("#10b981")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    val strokePaint = Paint().apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    canvas.rotate(rotation, size / 2f, size / 2f)

    val path = Path().apply {
        moveTo(size / 2f, 10f)
        lineTo(size - 20f, size - 20f)
        lineTo(size / 2f, size - 40f)
        lineTo(20f, size - 20f)
        close()
    }

    canvas.drawPath(path, paint)
    canvas.drawPath(path, strokePaint)

    return bitmap
}

/**
 * HUD Overlay with position data
 */
@Composable
fun HudOverlay(
    uiState: MapUiState,
    onFollowClick: () -> Unit,
    onControlDeckClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        CoordinatePanel(
            uiState = uiState,
            modifier = Modifier.align(Alignment.TopStart)
        )

        GpsStatusPanel(
            uiState = uiState,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        CompassPanel(
            compassData = uiState.compassData,
            modifier = Modifier.align(Alignment.BottomStart)
        )

        FloatingActionButton(
            onClick = onControlDeckClick,
            containerColor = Color(0xFF1e293b),
            contentColor = Color(0xFF10b981),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Control Deck"
            )
        }

        if (!uiState.isFollowingLocation) {
            FilledIconButton(
                onClick = onFollowClick,
                modifier = Modifier.align(Alignment.Center),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xCC1e293b)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Follow Location"
                )
            }
        }
    }
}

/**
 * Coordinate Panel (Top Left)
 */
@Composable
fun CoordinatePanel(
    uiState: MapUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = Color(0xCC0f172a),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = "WGS84",
            fontSize = 10.sp,
            color = Color(0xFF94a3b8),
            fontWeight = FontWeight.Bold
        )

        Row {
            Text(
                text = uiState.getFormattedLatitude(),
                fontSize = 14.sp,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = uiState.getFormattedLongitude(),
                fontSize = 14.sp,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "INDIAN GRID",
            fontSize = 10.sp,
            color = Color(0xFF94a3b8),
            fontWeight = FontWeight.Bold
        )

        Text(
            text = uiState.getFormattedGridCoordinates(),
            fontSize = 14.sp,
            color = Color(0xFF10b981),
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = "ZONE: ${uiState.getZoneDisplay()}",
            fontSize = 12.sp,
            color = Color(0xFFf59e0b),
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * GPS Status Panel (Top Right)
 */
@Composable
fun GpsStatusPanel(
    uiState: MapUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = Color(0xCC0f172a),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        val statusColor = when (uiState.gpsStatus) {
            MapUiState.GpsStatus.FIX_3D,
            MapUiState.GpsStatus.FIX_DIFFERENTIAL -> Color(0xFF10b981)
            MapUiState.GpsStatus.FIX_2D -> Color(0xFFf59e0b)
            else -> Color(0xFFef4444)
        }

        Text(
            text = uiState.getGpsStatusDisplay(),
            fontSize = 12.sp,
            color = statusColor,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = uiState.getFormattedAccuracy(),
            fontSize = 14.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = uiState.getFormattedAltitude(),
            fontSize = 14.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = "SAT: ${uiState.getSatelliteInfo()}",
            fontSize = 12.sp,
            color = Color(0xFF94a3b8),
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Compass Panel (Bottom Left)
 */
@Composable
fun CompassPanel(
    compassData: CompassSensorManager.CompassData?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(100.dp)
            .background(
                color = Color(0xCC0f172a),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .rotate(-(compassData?.azimuth ?: 0f))
            ) {
                Text(
                    text = "N",
                    fontSize = 16.sp,
                    color = Color(0xFFef4444),
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(-(compassData?.azimuth ?: 0f))
            ) {
                Text(
                    text = "S",
                    modifier = Modifier.align(Alignment.BottomCenter),
                    fontSize = 12.sp,
                    color = Color(0xFF94a3b8)
                )
                Text(
                    text = "E",
                    modifier = Modifier.align(Alignment.CenterEnd),
                    fontSize = 12.sp,
                    color = Color(0xFF94a3b8)
                )
                Text(
                    text = "W",
                    modifier = Modifier.align(Alignment.CenterStart),
                    fontSize = 12.sp,
                    color = Color(0xFF94a3b8)
                )
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = String.format("%.0f°", compassData?.azimuth ?: 0f),
                    fontSize = 18.sp,
                    color = Color(0xFFf59e0b),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = compassData?.getCardinalDirection() ?: "N",
                    fontSize = 10.sp,
                    color = Color(0xFF94a3b8)
                )
            }
        }
    }
}