package com.navigationpro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.UUID

/**
 * Waypoint entity for Room database
 * Stores navigation points with WGS84 coordinates and display properties
 */
@Entity(tableName = "waypoints")
data class Waypoint(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "altitude")
    val altitude: Double = 0.0,

    @ColumnInfo(name = "color")
    val color: WaypointColor = WaypointColor.RED,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "icon_type")
    val iconType: IconType = IconType.PIN
) {
    /**
     * Waypoint color options for map display
     */
    enum class WaypointColor {
        RED, GREEN, BLUE, YELLOW, PURPLE, ORANGE;

        fun toHexColor(): String {
            return when (this) {
                RED -> "#EF4444"
                GREEN -> "#10B981"
                BLUE -> "#3B82F6"
                YELLOW -> "#EAB308"
                PURPLE -> "#A855F7"
                ORANGE -> "#F97316"
            }
        }

        companion fun fromName(name: String): WaypointColor {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: RED
        }
    }

    /**
     * Icon type for waypoint display
     */
    enum class IconType {
        PIN, FLAG, STAR, CIRCLE, SQUARE, TRIANGLE;

        companion fun fromName(name: String): IconType {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: PIN
        }
    }

    /**
     * Get formatted coordinate string
     */
    fun getFormattedCoordinates(): String {
        return String.format("%.6f, %.6f", latitude, longitude)
    }

    /**
     * Create a copy with updated timestamp
     */
    fun copyWithTimestamp(): Waypoint {
        return copy(updatedAt = System.currentTimeMillis())
    }
}