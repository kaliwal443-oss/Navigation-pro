package com.navigationpro.presentation.state

import android.location.Location
import com.navigationpro.data.model.Waypoint
import com.navigationpro.domain.util.ProjectionMath
import com.navigationpro.domain.service.CompassSensorManager

/**
 * UI State for the Map Screen
 * Contains all data needed to render the map and HUD
 */
data class MapUiState(
    // Location data
    val currentLocation: Location? = null,
    val lastKnownLocation: Location? = null,
    val isLocationAvailable: Boolean = false,
    val locationAccuracy: Float = 0f,
    val satelliteCount: Int = 0,
    val satellitesUsedInFix: Int = 0,

    // Compass data
    val compassData: CompassSensorManager.CompassData? = null,
    val compassHeading: Float = 0f,

    // Grid coordinates
    val gridCoordinates: ProjectionMath.GridCoordinates? = null,
    val currentZone: ProjectionMath.GridZone? = null,

    // Map state
    val isFollowingLocation: Boolean = true,
    val currentZoom: Double = 15.0,
    val selectedMapLayer: MapLayer = MapLayer.TACTICAL_DARK,
    val isGridOverlayVisible: Boolean = true,

    // Waypoints
    val waypoints: List<Waypoint> = emptyList(),
    val selectedWaypoint: Waypoint? = null,

    // GPS Status
    val gpsStatus: GpsStatus = GpsStatus.SEARCHING,

    // UI State
    val isControlDeckOpen: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showPermissionRationale: Boolean = false,

    // Sun/Moon data
    val sunAltitude: Double = 0.0,
    val sunAzimuth: Double = 0.0,
    val moonAltitude: Double = 0.0,
    val moonAzimuth: Double = 0.0,
    val moonPhase: Double = 0.0
) {
    /**
     * GPS Status enumeration
     */
    enum class GpsStatus {
        SEARCHING,
        NO_FIX,
        FIX_2D,
        FIX_3D,
        FIX_DIFFERENTIAL
    }

    /**
     * Map layer options
     */
    enum class MapLayer(val displayName: String, val url: String) {
        TACTICAL_DARK(
            "Tactical Dark",
            "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
        ),
        SATELLITE(
            "Satellite",
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
        ),
        OPEN_TOPO(
            "Topographic",
            "https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png"
        ),
        STANDARD(
            "Standard",
            "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        );

        companion fun fromName(name: String): MapLayer {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: TACTICAL_DARK
        }
    }

    /**
     * Get formatted latitude string
     */
    fun getFormattedLatitude(): String {
        return currentLocation?.latitude?.let {
            ProjectionMath.formatCoordinate(it)
        } ?: "--.------"
    }

    /**
     * Get formatted longitude string
     */
    fun getFormattedLongitude(): String {
        return currentLocation?.longitude?.let {
            ProjectionMath.formatCoordinate(it)
        } ?: "--.------"
    }

    /**
     * Get formatted altitude string
     */
    fun getFormattedAltitude(): String {
        return currentLocation?.altitude?.let {
            String.format("%.1f m", it)
        } ?: "--- m"
    }

    /**
     * Get formatted accuracy string
     */
    fun getFormattedAccuracy(): String {
        return if (locationAccuracy > 0) {
            "±${locationAccuracy.toInt()} m"
        } else {
            "--- m"
        }
    }

    /**
     * Get formatted speed string
     */
    fun getFormattedSpeed(): String {
        return currentLocation?.speed?.let {
            val kmh = it * 3.6f
            String.format("%.1f km/h", kmh)
        } ?: "-- km/h"
    }

    /**
     * Get formatted heading string
     */
    fun getFormattedHeading(): String {
        return compassData?.let {
            String.format("%.0f° %s", it.azimuth, it.getCardinalDirection())
        } ?: "---°"
    }

    /**
     * Get formatted grid coordinates string
     */
    fun getFormattedGridCoordinates(): String {
        return gridCoordinates?.let {
            "E: ${it.easting.toInt()}  N: ${it.northing.toInt()}"
        } ?: "E: -------  N: -------"
    }

    /**
     * Get zone display string
     */
    fun getZoneDisplay(): String {
        return currentZone?.zoneName ?: "--"
    }

    /**
     * Get GPS status display string
     */
    fun getGpsStatusDisplay(): String {
        return when (gpsStatus) {
            GpsStatus.SEARCHING -> "SEARCHING..."
            GpsStatus.NO_FIX -> "NO FIX"
            GpsStatus.FIX_2D -> "2D FIX"
            GpsStatus.FIX_3D -> "3D FIX"
            GpsStatus.FIX_DIFFERENTIAL -> "DGPS"
        }
    }

    /**
     * Check if GPS has a valid fix
     */
    fun hasGpsFix(): Boolean {
        return gpsStatus == GpsStatus.FIX_2D ||
               gpsStatus == GpsStatus.FIX_3D ||
               gpsStatus == GpsStatus.FIX_DIFFERENTIAL
    }

    /**
     * Get satellite info string
     */
    fun getSatelliteInfo(): String {
        return "$satellitesUsedInFix/$satelliteCount"
    }
}