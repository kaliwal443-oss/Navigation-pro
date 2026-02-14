package com.navigationpro.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.navigationpro.data.model.Waypoint
import com.navigationpro.presentation.state.MapUiState
import com.navigationpro.presentation.viewmodel.MapViewModel
import kotlinx.coroutines.launch

/**
 * Control Deck Bottom Sheet Content with Tabs
 */
@Composable
fun ControlDeckContent(
    uiState: MapUiState,
    viewModel: MapViewModel,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(ControlDeckTab.TOOLS) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONTROL DECK",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10b981),
                fontFamily = FontFamily.Monospace
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF94a3b8)
                )
            }
        }

        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = Color(0xFF1e293b),
            contentColor = Color(0xFF10b981)
        ) {
            ControlDeckTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            ControlDeckTab.TOOLS -> ToolsTab(uiState = uiState)
            ControlDeckTab.PLAN -> PlanTab(uiState = uiState, viewModel = viewModel)
            ControlDeckTab.MAP -> MapTab(uiState = uiState, viewModel = viewModel)
        }
    }
}

/**
 * Control Deck Tabs
 */
enum class ControlDeckTab(val displayName: String) {
    TOOLS("TOOLS"),
    PLAN("PLAN"),
    MAP("MAP")
}

@Composable
fun ToolsTab(uiState: MapUiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1e293b))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "COMPASS",
                    fontSize = 12.sp,
                    color = Color(0xFF94a3b8),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CompassDataItem("AZIMUTH", String.format("%.1f°", uiState.compassHeading))
                    CompassDataItem("PITCH", String.format("%.1f°", uiState.compassData?.pitch ?: 0f))
                    CompassDataItem("ROLL", String.format("%.1f°", uiState.compassData?.roll ?: 0f))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1e293b))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SATELLITES",
                    fontSize = 12.sp,
                    color = Color(0xFF94a3b8),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CompassDataItem("TOTAL", uiState.satelliteCount.toString())
                    CompassDataItem("IN FIX", uiState.satellitesUsedInFix.toString())
                    CompassDataItem("SIGNAL", if (uiState.hasGpsFix()) "GOOD" else "WEAK")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1e293b))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ALMANAC",
                    fontSize = 12.sp,
                    color = Color(0xFF94a3b8),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CompassDataItem("SUN AZ", String.format("%.0f°", uiState.sunAzimuth))
                    CompassDataItem("SUN ALT", String.format("%.0f°", uiState.sunAltitude))
                    CompassDataItem("MOON AZ", String.format("%.0f°", uiState.moonAzimuth))
                }
            }
        }
    }
}

@Composable
fun CompassDataItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF64748b)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = Color(0xFF10b981),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun PlanTab(
    uiState: MapUiState,
    viewModel: MapViewModel
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionButton(
                icon = Icons.Default.AddLocation,
                label = "DROP MARKER",
                onClick = {
                    uiState.currentLocation?.let {
                        viewModel.createWaypointAtCurrentLocation(
                            "WP-${System.currentTimeMillis() % 10000}"
                        )
                    }
                }
            )

            ActionButton(
                icon = Icons.Default.Edit,
                label = "ADD MANUAL",
                onClick = { showAddDialog = true }
            )

            ActionButton(
                icon = Icons.Default.Share,
                label = "EXPORT GPX",
                onClick = {
                    viewModel.exportWaypointsToGpx { result ->
                        // Handle export result
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "WAYPOINTS (${uiState.waypoints.size})",
            fontSize = 12.sp,
            color = Color(0xFF94a3b8),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
            items(uiState.waypoints, key = { it.id }) { waypoint ->
                WaypointItem(
                    waypoint = waypoint,
                    onDelete = { viewModel.deleteWaypoint(waypoint) },
                    onSelect = { viewModel.selectWaypoint(waypoint) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddWaypointDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, lat, lng, color ->
                viewModel.createWaypoint(name, lat, lng, color)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = Color(0xFF334155),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF10b981)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF94a3b8)
        )
    }
}

@Composable
fun WaypointItem(
    waypoint: Waypoint,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = Color(android.graphics.Color.parseColor(waypoint.color.toHexColor())),
                            shape = CircleShape
                        )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = waypoint.name,
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = waypoint.getFormattedCoordinates(),
                        fontSize = 12.sp,
                        color = Color(0xFF94a3b8),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFef4444)
                )
            }
        }
    }
}

@Composable
fun AddWaypointDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, lat: Double, lng: Double, color: Waypoint.WaypointColor) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Waypoint.WaypointColor.RED) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1e293b),
        title = {
            Text(
                text = "ADD WAYPOINT",
                color = Color(0xFF10b981),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", color = Color(0xFF94a3b8)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10b981),
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = lat,
                    onValueChange = { lat = it },
                    label = { Text("Latitude", color = Color(0xFF94a3b8)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10b981),
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = lng,
                    onValueChange = { lng = it },
                    label = { Text("Longitude", color = Color(0xFF94a3b8)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10b981),
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "COLOR",
                    fontSize = 12.sp,
                    color = Color(0xFF94a3b8)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Waypoint.WaypointColor.entries.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    Color(android.graphics.Color.parseColor(color.toHexColor()))
                                )
                                .clickable { selectedColor = color }
                                .then(
                                    if (selectedColor == color) {
                                        Modifier.padding(2.dp)
                                    } else Modifier
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val latVal = lat.toDoubleOrNull()
                    val lngVal = lng.toDoubleOrNull()
                    if (name.isNotBlank() && latVal != null && lngVal != null) {
                        onConfirm(name, latVal, lngVal, selectedColor)
                    }
                }
            ) {
                Text("ADD", color = Color(0xFF10b981))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color(0xFF94a3b8))
            }
        }
    )
}

@Composable
fun MapTab(
    uiState: MapUiState,
    viewModel: MapViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "MAP LAYER",
            fontSize = 12.sp,
            color = Color(0xFF94a3b8),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        MapUiState.MapLayer.entries.forEach { layer ->
            LayerOption(
                name = layer.displayName,
                isSelected = uiState.selectedMapLayer == layer,
                onClick = { viewModel.setMapLayer(layer) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.toggleGridOverlay() }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INDIAN GRID OVERLAY",
                fontSize = 14.sp,
                color = Color.White
            )

            Switch(
                checked = uiState.isGridOverlayVisible,
                onCheckedChange = { viewModel.toggleGridOverlay() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF10b981),
                    checkedTrackColor = Color(0xFF059669)
                )
            )
        }
    }
}

@Composable
fun LayerOption(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF059669) else Color(0xFF334155)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                fontSize = 14.sp,
                color = Color.White
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFF10b981)
                )
            }
        }
    }
}