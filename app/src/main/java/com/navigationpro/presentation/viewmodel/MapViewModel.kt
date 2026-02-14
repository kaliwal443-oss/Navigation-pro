package com.navigationpro.presentation.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.navigationpro.data.model.Waypoint
import com.navigationpro.domain.repository.WaypointRepository
import com.navigationpro.domain.service.CompassSensorManager
import com.navigationpro.domain.service.GpxService
import com.navigationpro.domain.util.ProjectionMath
import com.navigationpro.presentation.state.MapUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.shredzone.commons.suncalc.MoonPosition
import org.shredzone.commons.suncalc.SunPosition
import java.time.ZonedDateTime
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for the Map Screen
 * Manages location updates, compass data, waypoints, and UI state
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    application: Application,
    private val waypointRepository: WaypointRepository,
    private val gpxService: GpxService,
    private val compassSensorManager: CompassSensorManager
) : AndroidViewModel(application) {

    // Fused Location Provider
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    // Location Manager for satellite info
    private val locationManager = application.getSystemService(LocationManager::class.java)

    // Location callback for fused location updates
    private var locationCallback: LocationCallback? = null

    // GPS status listener
    private var gpsStatusListener: android.location.GpsStatus.Listener? = null

    // Private mutable state
    private val _uiState = MutableStateFlow(MapUiState())

    // Public immutable state
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    // Waypoints flow
    val waypoints: StateFlow<List<Waypoint>> = waypointRepository.getAllWaypoints()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Collect waypoints and update state
        viewModelScope.launch {
            waypoints.collect { waypoints ->
                _uiState.update { it.copy(waypoints = waypoints) }
            }
        }

        // Start compass updates
        startCompassUpdates()

        // Update sun/moon position periodically
        startSunMoonUpdates()
    }

    /**
     * Start receiving location updates
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L // Update interval 1 second
        ).apply {
            setMinUpdateIntervalMillis(500L)
            setWaitForAccurateLocation(true)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    updateLocation(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )

            // Also listen for GPS status changes
            startGpsStatusListener()
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Failed to start location updates: ${e.message}") }
        }
    }

    /**
     * Stop location updates
     */
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null

        gpsStatusListener?.let {
            locationManager?.unregisterGpsStatusListener(it)
        }
        gpsStatusListener = null
    }

    /**
     * Start GPS status listener for satellite info
     */
    @SuppressLint("MissingPermission")
    private fun startGpsStatusListener() {
        try {
            gpsStatusListener = android.location.GpsStatus.Listener { event ->
                when (event) {
                    android.location.GpsStatus.GPS_EVENT_SATELLITE_STATUS -> {
                        updateSatelliteInfo()
                    }
                    android.location.GpsStatus.GPS_EVENT_FIRST_FIX -> {
                        _uiState.update { it.copy(gpsStatus = MapUiState.GpsStatus.FIX_3D) }
                    }
                }
            }

            locationManager?.addGpsStatusListener(gpsStatusListener)
        } catch (e: Exception) {
            // GPS status not available
        }
    }

    /**
     * Update satellite information
     */
    @SuppressLint("MissingPermission")
    private fun updateSatelliteInfo() {
        try {
            val gpsStatus = locationManager?.getGpsStatus(null)
            val satellites = gpsStatus?.satellites

            var totalSatellites = 0
            var satellitesInFix = 0

            satellites?.let {
                for (satellite in it) {
                    totalSatellites++
                    if (satellite.usedInFix()) {
                        satellitesInFix++
                    }
                }
            }

            _uiState.update {
                it.copy(
                    satelliteCount = totalSatellites,
                    satellitesUsedInFix = satellitesInFix
                )
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Update location and calculate grid coordinates
     */
    private fun updateLocation(location: Location) {
        val gridCoords = ProjectionMath.wgs84ToIndianGrid(
            location.latitude,
            location.longitude
        )

        val gpsStatus = when {
            !location.hasAccuracy() -> MapUiState.GpsStatus.NO_FIX
            location.hasAltitude() -> MapUiState.GpsStatus.FIX_3D
            else -> MapUiState.GpsStatus.FIX_2D
        }

        _uiState.update {
            it.copy(
                currentLocation = location,
                lastKnownLocation = location,
                isLocationAvailable = true,
                locationAccuracy = location.accuracy,
                gridCoordinates = gridCoords,
                currentZone = gridCoords?.zone,
                gpsStatus = gpsStatus
            )
        }
    }

    /**
     * Start compass sensor updates
     */
    private fun startCompassUpdates() {
        viewModelScope.launch {
            compassSensorManager.getCompassDataFlow()
                .collect { compassData ->
                    _uiState.update {
                        it.copy(
                            compassData = compassData,
                            compassHeading = compassData.azimuth
                        )
                    }
                }
        }
    }

    /**
     * Start periodic sun/moon position updates
     */
    private fun startSunMoonUpdates() {
        viewModelScope.launch {
            while (true) {
                updateSunMoonPosition()
                kotlinx.coroutines.delay(60000) // Update every minute
            }
        }
    }

    /**
     * Calculate sun and moon position
     */
    private fun updateSunMoonPosition() {
        val location = _uiState.value.currentLocation ?: return

        val now = ZonedDateTime.now()

        // Calculate sun position
        val sunPos = SunPosition.compute()
            .at(location.latitude, location.longitude)
            .timezone(java.util.TimeZone.getDefault().toZoneId())
            .now()
            .execute()

        // Calculate moon position
        val moonPos = MoonPosition.compute()
            .at(location.latitude, location.longitude)
            .timezone(java.util.TimeZone.getDefault().toZoneId())
            .now()
            .execute()

        _uiState.update {
            it.copy(
                sunAltitude = sunPos.altitude,
                sunAzimuth = sunPos.azimuth,
                moonAltitude = moonPos.altitude,
                moonAzimuth = moonPos.azimuth,
                moonPhase = moonPos.illumination
            )
        }
    }

    /**
     * Toggle map layer
     */
    fun setMapLayer(layer: MapUiState.MapLayer) {
        _uiState.update { it.copy(selectedMapLayer = layer) }
    }

    /**
     * Toggle grid overlay visibility
     */
    fun toggleGridOverlay() {
        _uiState.update { it.copy(isGridOverlayVisible = !it.isGridOverlayVisible) }
    }

    /**
     * Toggle follow location mode
     */
    fun toggleFollowLocation() {
        _uiState.update { it.copy(isFollowingLocation = !it.isFollowingLocation) }
    }

    /**
     * Set follow location mode
     */
    fun setFollowLocation(follow: Boolean) {
        _uiState.update { it.copy(isFollowingLocation = follow) }
    }

    /**
     * Update zoom level
     */
    fun updateZoom(zoom: Double) {
        _uiState.update { it.copy(currentZoom = zoom) }
    }

    /**
     * Open control deck
     */
    fun openControlDeck() {
        _uiState.update { it.copy(isControlDeckOpen = true) }
    }

    /**
     * Close control deck
     */
    fun closeControlDeck() {
        _uiState.update { it.copy(isControlDeckOpen = false) }
    }

    /**
     * Create a waypoint at current location
     */
    fun createWaypointAtCurrentLocation(name: String, color: Waypoint.WaypointColor = Waypoint.WaypointColor.RED) {
        viewModelScope.launch {
            val location = _uiState.value.currentLocation ?: return@launch

            waypointRepository.createWaypoint(
                name = name,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                color = color
            )
        }
    }

    /**
     * Create a waypoint at specified coordinates
     */
    fun createWaypoint(
        name: String,
        latitude: Double,
        longitude: Double,
        color: Waypoint.WaypointColor = Waypoint.WaypointColor.RED
    ) {
        viewModelScope.launch {
            waypointRepository.createWaypoint(
                name = name,
                latitude = latitude,
                longitude = longitude,
                color = color
            )
        }
    }

    /**
     * Create waypoint from Indian Grid coordinates
     */
    fun createWaypointFromGrid(
        name: String,
        easting: Double,
        northing: Double,
        zone: ProjectionMath.GridZone,
        color: Waypoint.WaypointColor = Waypoint.WaypointColor.RED
    ) {
        viewModelScope.launch {
            val wgs84 = ProjectionMath.indianGridToWgs84(easting, northing, zone)
                ?: return@launch

            waypointRepository.createWaypoint(
                name = name,
                latitude = wgs84.first,
                longitude = wgs84.second,
                color = color
            )
        }
    }

    /**
     * Delete a waypoint
     */
    fun deleteWaypoint(waypoint: Waypoint) {
        viewModelScope.launch {
            waypointRepository.deleteWaypoint(waypoint)
        }
    }

    /**
     * Delete waypoint by ID
     */
    fun deleteWaypointById(id: String) {
        viewModelScope.launch {
            waypointRepository.deleteWaypointById(id)
        }
    }

    /**
     * Toggle waypoint favorite status
     */
    fun toggleWaypointFavorite(id: String) {
        viewModelScope.launch {
            waypointRepository.toggleFavorite(id)
        }
    }

    /**
     * Export waypoints to GPX
     */
    fun exportWaypointsToGpx(fileName: String? = null, onComplete: (Result<android.net.Uri>) -> Unit) {
        viewModelScope.launch {
            val waypoints = waypointRepository.getAllWaypointsList()
            val result = gpxService.exportWaypoints(waypoints, fileName)
            onComplete(result)
        }
    }

    /**
     * Import waypoints from GPX
     */
    fun importWaypointsFromGpx(uri: android.net.Uri, onComplete: (Result<List<Waypoint>>) -> Unit) {
        viewModelScope.launch {
            val result = gpxService.importWaypoints(uri)
            result.onSuccess { waypoints ->
                waypointRepository.insertWaypoints(waypoints)
            }
            onComplete(result)
        }
    }

    /**
     * Select a waypoint
     */
    fun selectWaypoint(waypoint: Waypoint?) {
        _uiState.update { it.copy(selectedWaypoint = waypoint) }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Show permission rationale
     */
    fun showPermissionRationale() {
        _uiState.update { it.copy(showPermissionRationale = true) }
    }

    /**
     * Hide permission rationale
     */
    fun hidePermissionRationale() {
        _uiState.update { it.copy(showPermissionRationale = false) }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}