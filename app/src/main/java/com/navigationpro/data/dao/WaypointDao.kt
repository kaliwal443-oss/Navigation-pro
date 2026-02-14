package com.navigationpro.data.dao

import androidx.room.*
import com.navigationpro.data.model.Waypoint
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Waypoint operations
 */
@Dao
interface WaypointDao {

    /**
     * Get all waypoints ordered by creation date
     */
    @Query("SELECT * FROM waypoints ORDER BY created_at DESC")
    fun getAllWaypoints(): Flow<List<Waypoint>>

    /**
     * Get all waypoints as a list (non-flow)
     */
    @Query("SELECT * FROM waypoints ORDER BY created_at DESC")
    suspend fun getAllWaypointsList(): List<Waypoint>

    /**
     * Get waypoint by ID
     */
    @Query("SELECT * FROM waypoints WHERE id = :id")
    suspend fun getWaypointById(id: String): Waypoint?

    /**
     * Get favorite waypoints
     */
    @Query("SELECT * FROM waypoints WHERE is_favorite = 1 ORDER BY created_at DESC")
    fun getFavoriteWaypoints(): Flow<List<Waypoint>>

    /**
     * Search waypoints by name
     */
    @Query("SELECT * FROM waypoints WHERE name LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun searchWaypoints(query: String): Flow<List<Waypoint>>

    /**
     * Get waypoints within bounding box
     */
    @Query("""
        SELECT * FROM waypoints 
        WHERE latitude BETWEEN :minLat AND :maxLat 
        AND longitude BETWEEN :minLng AND :maxLng
    """)
    suspend fun getWaypointsInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): List<Waypoint>

    /**
     * Insert a new waypoint
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: Waypoint)

    /**
     * Insert multiple waypoints
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoints(waypoints: List<Waypoint>)

    /**
     * Update an existing waypoint
     */
    @Update
    suspend fun updateWaypoint(waypoint: Waypoint)

    /**
     * Delete a waypoint
     */
    @Delete
    suspend fun deleteWaypoint(waypoint: Waypoint)

    /**
     * Delete waypoint by ID
     */
    @Query("DELETE FROM waypoints WHERE id = :id")
    suspend fun deleteWaypointById(id: String)

    /**
     * Delete all waypoints
     */
    @Query("DELETE FROM waypoints")
    suspend fun deleteAllWaypoints()

    /**
     * Get waypoint count
     */
    @Query("SELECT COUNT(*) FROM waypoints")
    suspend fun getWaypointCount(): Int

    /**
     * Toggle favorite status
     */
    @Query("UPDATE waypoints SET is_favorite = NOT is_favorite WHERE id = :id")
    suspend fun toggleFavorite(id: String)

    /**
     * Update waypoint color
     */
    @Query("UPDATE waypoints SET color = :color WHERE id = :id")
    suspend fun updateWaypointColor(id: String, color: Waypoint.WaypointColor)

    /**
     * Get waypoints by color
     */
    @Query("SELECT * FROM waypoints WHERE color = :color ORDER BY created_at DESC")
    fun getWaypointsByColor(color: Waypoint.WaypointColor): Flow<List<Waypoint>>
}