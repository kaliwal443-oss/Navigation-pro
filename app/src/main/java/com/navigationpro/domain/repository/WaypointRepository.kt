package com.navigationpro.domain.repository

import com.navigationpro.data.dao.WaypointDao
import com.navigationpro.data.model.Waypoint
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Waypoint operations
 * Provides clean API for data operations and abstracts data sources
 */
@Singleton
class WaypointRepository @Inject constructor(
    private val waypointDao: WaypointDao
) {
    /**
     * Get all waypoints as Flow
     */
    fun getAllWaypoints(): Flow<List<Waypoint>> = waypointDao.getAllWaypoints()

    /**
     * Get all waypoints as list
     */
    suspend fun getAllWaypointsList(): List<Waypoint> = waypointDao.getAllWaypointsList()

    /**
     * Get waypoint by ID
     */
    suspend fun getWaypointById(id: String): Waypoint? = waypointDao.getWaypointById(id)

    /**
     * Get favorite waypoints
     */
    fun getFavoriteWaypoints(): Flow<List<Waypoint>> = waypointDao.getFavoriteWaypoints()

    /**
     * Search waypoints by name
     */
    fun searchWaypoints(query: String): Flow<List<Waypoint>> = waypointDao.searchWaypoints(query)

    /**
     * Get waypoints within bounding box
     */
    suspend fun getWaypointsInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): List<Waypoint> = waypointDao.getWaypointsInBounds(minLat, maxLat, minLng, maxLng)

    /**
     * Insert a new waypoint
     */
    suspend fun insertWaypoint(waypoint: Waypoint) {
        waypointDao.insertWaypoint(waypoint)
    }

    /**
     * Insert multiple waypoints
     */
    suspend fun insertWaypoints(waypoints: List<Waypoint>) {
        waypointDao.insertWaypoints(waypoints)
    }

    /**
     * Update an existing waypoint
     */
    suspend fun updateWaypoint(waypoint: Waypoint) {
        waypointDao.updateWaypoint(waypoint.copyWithTimestamp())
    }

    /**
     * Delete a waypoint
     */
    suspend fun deleteWaypoint(waypoint: Waypoint) {
        waypointDao.deleteWaypoint(waypoint)
    }

    /**
     * Delete waypoint by ID
     */
    suspend fun deleteWaypointById(id: String) {
        waypointDao.deleteWaypointById(id)
    }

    /**
     * Delete all waypoints
     */
    suspend fun deleteAllWaypoints() {
        waypointDao.deleteAllWaypoints()
    }

    /**
     * Get waypoint count
     */
    suspend fun getWaypointCount(): Int = waypointDao.getWaypointCount()

    /**
     * Toggle favorite status
     */
    suspend fun toggleFavorite(id: String) {
        waypointDao.toggleFavorite(id)
    }

    /**
     * Update waypoint color
     */
    suspend fun updateWaypointColor(id: String, color: Waypoint.WaypointColor) {
        waypointDao.updateWaypointColor(id, color)
    }

    /**
     * Get waypoints by color
     */
    fun getWaypointsByColor(color: Waypoint.WaypointColor): Flow<List<Waypoint>> {
        return waypointDao.getWaypointsByColor(color)
    }

    /**
     * Create a new waypoint at specified coordinates
     */
    suspend fun createWaypoint(
        name: String,
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0,
        color: Waypoint.WaypointColor = Waypoint.WaypointColor.RED,
        description: String = ""
    ): Waypoint {
        val waypoint = Waypoint(
            name = name,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            color = color,
            description = description
        )
        waypointDao.insertWaypoint(waypoint)
        return waypoint
    }
}