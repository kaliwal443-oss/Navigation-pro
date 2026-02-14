package com.navigationpro.domain.util

import kotlin.math.*

/**
 * Indian Grid System (IGS) Projection Mathematics
 *
 * Converts WGS84 (Lat/Lng) coordinates to Indian Grid System using
 * Lambert Conformal Conic (LCC) projection with Everest 1830 ellipsoid.
 *
 * Reference: Survey of India - Indian Grid System Specifications
 *
 * Ellipsoid: Everest 1830
 *   - Semi-major axis (a): 6377276.345 meters
 *   - Inverse flattening (1/f): 300.8017
 *   - Flattening (f): 1/300.8017
 *   - Eccentricity squared (e²): 2f - f²
 *
 * Projection: Lambert Conformal Conic (LCC)
 *   - Scale factor at origin (k0): 0.998786408
 *   - False Easting: 2,743,195.61 meters
 *   - False Northing: 914,398.54 meters
 */
object ProjectionMath {

    // Everest 1830 Ellipsoid Parameters
    private const val EVEREST_A = 6377276.345 // Semi-major axis in meters
    private const val EVEREST_INV_F = 300.8017 // Inverse flattening
    private const val EVEREST_F = 1.0 / EVEREST_INV_F
    private const val EVEREST_E2 = 2 * EVEREST_F - EVEREST_F * EVEREST_F // Eccentricity squared
    private const val EVEREST_E = sqrt(EVEREST_E2)

    // LCC Projection Constants
    private const val K0 = 0.998786408 // Scale factor at origin
    private const val FALSE_EASTING = 2743195.61
    private const val FALSE_NORTHING = 914398.54

    /**
     * Indian Grid Zones with their origin points (latitude, longitude)
     */
    enum class GridZone(
        val zoneName: String,
        val originLat: Double,  // degrees
        val originLng: Double,  // degrees
        val minLat: Double,
        val maxLat: Double,
        val minLng: Double,
        val maxLng: Double
    ) {
        ZONE_0(
            "Zone 0",
            39.5, 68.0,
            35.0, 42.0,
            64.0, 72.0
        ),
        ZONE_I(
            "Zone I",
            32.5, 68.0,
            28.0, 36.0,
            64.0, 72.0
        ),
        ZONE_IIA(
            "Zone IIA",
            26.0, 74.0,
            21.0, 29.0,
            72.0, 78.0
        ),
        ZONE_IIB(
            "Zone IIB",
            26.0, 84.0,
            21.0, 29.0,
            80.0, 88.0
        ),
        ZONE_IIIA(
            "Zone IIIA",
            19.0, 80.0,
            15.0, 23.0,
            76.0, 84.0
        ),
        ZONE_IIIB(
            "Zone IIIB",
            19.0, 84.0,
            15.0, 23.0,
            82.0, 90.0
        ),
        ZONE_IVA(
            "Zone IVA",
            12.0, 80.0,
            8.0, 16.0,
            76.0, 84.0
        ),
        ZONE_IVB(
            "Zone IVB",
            12.0, 84.0,
            8.0, 16.0,
            82.0, 90.0
        );

        companion object {
            /**
             * Automatically determine the grid zone based on WGS84 coordinates
             */
            fun fromCoordinates(latitude: Double, longitude: Double): GridZone? {
                return entries.find { zone ->
                    latitude in zone.minLat..zone.maxLat &&
                    longitude in zone.minLng..zone.maxLng
                }
            }
        }
    }

    /**
     * Result of coordinate conversion
     */
    data class GridCoordinates(
        val easting: Double,
        val northing: Double,
        val zone: GridZone,
        val convergence: Double, // Grid convergence in degrees
        val scaleFactor: Double
    )

    /**
     * Convert WGS84 (Lat/Lng) to Indian Grid System (Easting/Northing)
     *
     * @param latitude WGS84 latitude in decimal degrees
     * @param longitude WGS84 longitude in decimal degrees
     * @param zone Optional grid zone (auto-detected if null)
     * @return GridCoordinates or null if outside all zones
     */
    fun wgs84ToIndianGrid(
        latitude: Double,
        longitude: Double,
        zone: GridZone? = null
    ): GridCoordinates? {
        val targetZone = zone ?: GridZone.fromCoordinates(latitude, longitude)
            ?: return null

        // Convert to radians
        val latRad = Math.toRadians(latitude)
        val lngRad = Math.toRadians(longitude)
        val lat0Rad = Math.toRadians(targetZone.originLat)
        val lng0Rad = Math.toRadians(targetZone.originLng)

        // Calculate isometric latitude
        val isoLat = isometricLatitude(latRad)
        val isoLat0 = isometricLatitude(lat0Rad)

        // Calculate radius of curvature in the prime vertical
        val nu = EVEREST_A / sqrt(1 - EVEREST_E2 * sin(latRad).pow(2))
        val nu0 = EVEREST_A / sqrt(1 - EVEREST_E2 * sin(lat0Rad).pow(2))

        // LCC projection parameters
        // Standard parallels are typically at 1/6 and 5/6 of the zone's latitudinal extent
        val latRange = targetZone.maxLat - targetZone.minLat
        val phi1 = Math.toRadians(targetZone.minLat + latRange / 6)
        val phi2 = Math.toRadians(targetZone.maxLat - latRange / 6)

        // Calculate cone constant (n)
        val n = ln(cos(phi1) / cos(phi2)) /
                ln(tan(Math.PI / 4 + phi2 / 2) / tan(Math.PI / 4 + phi1 / 2))

        // Calculate radius at standard parallel
        val F = (cos(phi1) * tan(Math.PI / 4 + phi1 / 2).pow(n)) / n

        // Calculate radius at origin and point
        val rho0 = EVEREST_A * F / tan(Math.PI / 4 + lat0Rad / 2).pow(n)
        val rho = EVEREST_A * F / tan(Math.PI / 4 + latRad / 2).pow(n)

        // Calculate angle theta
        val theta = n * (lngRad - lng0Rad)

        // Calculate easting and northing
        val easting = FALSE_EASTING + rho * sin(theta) * K0
        val northing = FALSE_NORTHING + rho0 - rho * cos(theta) * K0

        // Calculate grid convergence
        val convergence = Math.toDegrees(theta)

        // Calculate scale factor
        val scaleFactor = (n * rho / (EVEREST_A * cos(latRad))) * K0

        return GridCoordinates(
            easting = easting,
            northing = northing,
            zone = targetZone,
            convergence = convergence,
            scaleFactor = scaleFactor
        )
    }

    /**
     * Convert Indian Grid System (Easting/Northing) to WGS84 (Lat/Lng)
     *
     * @param easting Grid easting in meters
     * @param northing Grid northing in meters
     * @param zone The grid zone
     * @return Pair of (latitude, longitude) in decimal degrees, or null if invalid
     */
    fun indianGridToWgs84(
        easting: Double,
        northing: Double,
        zone: GridZone
    ): Pair<Double, Double>? {
        // Validate coordinates are within reasonable bounds
        if (easting < 0 || northing < 0) return null

        // Convert origin to radians
        val lat0Rad = Math.toRadians(zone.originLat)
        val lng0Rad = Math.toRadians(zone.originLng)

        // Calculate LCC parameters (same as forward conversion)
        val latRange = zone.maxLat - zone.minLat
        val phi1 = Math.toRadians(zone.minLat + latRange / 6)
        val phi2 = Math.toRadians(zone.maxLat - latRange / 6)

        val n = ln(cos(phi1) / cos(phi2)) /
                ln(tan(Math.PI / 4 + phi2 / 2) / tan(Math.PI / 4 + phi1 / 2))

        val F = (cos(phi1) * tan(Math.PI / 4 + phi1 / 2).pow(n)) / n
        val rho0 = EVEREST_A * F / tan(Math.PI / 4 + lat0Rad / 2).pow(n)

        // Reverse calculations
        val eastingAdjusted = (easting - FALSE_EASTING) / K0
        val northingAdjusted = (rho0 - (northing - FALSE_NORTHING) / K0)

        // Calculate theta
        val theta = atan2(eastingAdjusted, northingAdjusted)

        // Calculate longitude
        val lngRad = lng0Rad + theta / n

        // Calculate latitude using iterative method
        val rho = sqrt(eastingAdjusted.pow(2) + northingAdjusted.pow(2))
        val t = (EVEREST_A * F / rho).pow(1 / n)

        // Initial latitude estimate
        var latRad = Math.PI / 2 - 2 * atan(t)

        // Iterate for better accuracy (3 iterations usually sufficient)
        repeat(3) {
            val esinLat = EVEREST_E * sin(latRad)
            val newLat = Math.PI / 2 - 2 * atan(
                t * ((1 - esinLat) / (1 + esinLat)).pow(EVEREST_E / 2)
            )
            if (abs(newLat - latRad) < 1e-12) {
                latRad = newLat
                return@repeat
            }
            latRad = newLat
        }

        val latitude = Math.toDegrees(latRad)
        val longitude = Math.toDegrees(lngRad)

        // Validate result is within zone bounds
        return if (latitude in zone.minLat..zone.maxLat &&
                   longitude in zone.minLng..zone.maxLng) {
            Pair(latitude, longitude)
        } else {
            null
        }
    }

    /**
     * Calculate isometric latitude (conformal latitude)
     */
    private fun isometricLatitude(latRad: Double): Double {
        val sinLat = sin(latRad)
        val esinLat = EVEREST_E * sinLat
        return ln(tan(Math.PI / 4 + latRad / 2) *
                  ((1 - esinLat) / (1 + esinLat)).pow(EVEREST_E / 2))
    }

    /**
     * Format grid coordinates for display
     */
    fun formatGridCoordinates(coordinates: GridCoordinates): String {
        return "E: ${coordinates.easting.toInt()}  N: ${coordinates.northing.toInt()}"
    }

    /**
     * Format coordinates in MGRS-style compact format
     */
    fun formatCompact(easting: Double, northing: Double): String {
        val e = (easting / 1000).toInt()
        val n = (northing / 1000).toInt()
        return "${e}E ${n}N"
    }

    /**
     * Calculate distance between two WGS84 points using Haversine formula
     */
    fun haversineDistance(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val R = 6371000.0 // Earth's radius in meters

        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLng = Math.toRadians(lng2 - lng1)

        val a = sin(deltaLat / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }

    /**
     * Calculate initial bearing from point 1 to point 2
     */
    fun initialBearing(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLng = Math.toRadians(lng2 - lng1)

        val y = sin(deltaLng) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) -
                sin(lat1Rad) * cos(lat2Rad) * cos(deltaLng)

        val bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360) % 360
    }

    /**
     * Convert decimal degrees to DMS (Degrees, Minutes, Seconds)
     */
    fun decimalToDMS(decimal: Double, isLatitude: Boolean): String {
        val absolute = abs(decimal)
        val degrees = absolute.toInt()
        val minutesFull = (absolute - degrees) * 60
        val minutes = minutesFull.toInt()
        val seconds = (minutesFull - minutes) * 60

        val direction = when {
            isLatitude -> if (decimal >= 0) "N" else "S"
            else -> if (decimal >= 0) "E" else "W"
        }

        return String.format("%02d°%02d'%05.2f\"%s", degrees, minutes, seconds, direction)
    }

    /**
     * Convert DMS to decimal degrees
     */
    fun dmsToDecimal(degrees: Int, minutes: Int, seconds: Double, direction: String): Double {
        var decimal = degrees + minutes / 60.0 + seconds / 3600.0
        if (direction == "S" || direction == "W") {
            decimal = -decimal
        }
        return decimal
    }

    /**
     * Format coordinate for display with specified precision
     */
    fun formatCoordinate(value: Double, precision: Int = 6): String {
        return String.format("%.${precision}f", value)
    }

    /**
     * Check if coordinates are within valid Indian Grid bounds
     */
    fun isWithinIndianGridBounds(latitude: Double, longitude: Double): Boolean {
        return GridZone.fromCoordinates(latitude, longitude) != null
    }

    /**
     * Get zone description for display
     */
    fun getZoneDescription(zone: GridZone): String {
        return "${zone.zoneName} (${zone.originLat}°N, ${zone.originLng}°E)"
    }
}