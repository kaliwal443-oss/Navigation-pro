package com.navigationpro.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.navigationpro.data.dao.WaypointDao
import com.navigationpro.data.model.Waypoint

/**
 * Room Database for Navigation Pro
 * Stores waypoints and application settings
 */
@Database(
    entities = [Waypoint::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NavigationDatabase : RoomDatabase() {

    abstract fun waypointDao(): WaypointDao

    companion object {
        private const val DATABASE_NAME = "navigation_pro.db"

        @Volatile
        private var instance: NavigationDatabase? = null

        fun getInstance(context: Context): NavigationDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): NavigationDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                NavigationDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

/**
 * Type converters for Room database
 */
class Converters {

    @TypeConverter
    fun fromWaypointColor(color: Waypoint.WaypointColor): String {
        return color.name
    }

    @TypeConverter
    fun toWaypointColor(name: String): Waypoint.WaypointColor {
        return Waypoint.WaypointColor.fromName(name)
    }

    @TypeConverter
    fun fromIconType(iconType: Waypoint.IconType): String {
        return iconType.name
    }

    @TypeConverter
    fun toIconType(name: String): Waypoint.IconType {
        return Waypoint.IconType.fromName(name)
    }
}