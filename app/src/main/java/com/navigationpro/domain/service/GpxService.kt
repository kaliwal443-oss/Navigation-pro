package com.navigationpro.domain.service

import android.content.Context
import android.net.Uri
import com.navigationpro.data.model.Waypoint
import io.jenetics.jpx.GPX
import io.jenetics.jpx.WayPoint
import io.jenetics.jpx.Metadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for GPX file import/export operations
 */
@Singleton
class GpxService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val GPX_EXPORT_DIR = "NavigationPro/Exports"
        private const val GPX_MIME_TYPE = "application/gpx+xml"
    }

    /**
     * Export waypoints to GPX file
     *
     * @param waypoints List of waypoints to export
     * @param fileName Optional custom filename
     * @return Result containing the exported file URI or error
     */
    suspend fun exportWaypoints(
        waypoints: List<Waypoint>,
        fileName: String? = null
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (waypoints.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No waypoints to export"))
            }

            val timestamp = System.currentTimeMillis()
            val exportFileName = fileName ?: "waypoints_${timestamp}.gpx"

            // Create export directory
            val exportDir = File(context.getExternalFilesDir(null), GPX_EXPORT_DIR)
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val exportFile = File(exportDir, exportFileName)

            // Convert waypoints to JPX WayPoints
            val gpxWaypoints = waypoints.map { waypoint ->
                WayPoint.builder()
                    .lat(waypoint.latitude)
                    .lon(waypoint.longitude)
                    .apply {
                        waypoint.altitude.takeIf { it != 0.0 }?.let { ele(it) }
                        name(waypoint.name)
                        desc(waypoint.description.takeIf { it.isNotBlank() })
                        time(waypoint.createdAt.let {
                            ZonedDateTime.ofInstant(
                                Instant.ofEpochMilli(it),
                                ZoneId.systemDefault()
                            )
                        })
                        // Store color in extensions
                        extensions(mapOf(
                            "color" to waypoint.color.name,
                            "icon" to waypoint.iconType.name
                        ))
                    }
                    .build()
            }

            // Build GPX document
            val gpx = GPX.builder()
                .metadata(
                    Metadata.builder()
                        .name("Navigation Pro Waypoints")
                        .desc("Exported from Navigation Pro Tactical GPS")
                        .time(ZonedDateTime.now())
                        .build()
                )
                .addWayPoints(gpxWaypoints)
                .build()

            // Write to file
            GPX.write(gpx, exportFile.toPath())

            Result.success(Uri.fromFile(exportFile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import waypoints from GPX file
     *
     * @param uri URI of the GPX file to import
     * @return Result containing list of imported waypoints or error
     */
    suspend fun importWaypoints(uri: Uri): Result<List<Waypoint>> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalArgumentException("Cannot open file"))

            importFromStream(inputStream)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import waypoints from input stream
     */
    suspend fun importFromStream(inputStream: InputStream): Result<List<Waypoint>> =
        withContext(Dispatchers.IO) {
            try {
                val gpx = GPX.read(inputStream)

                val waypoints = gpx.wayPoints.map { gpxWaypoint ->
                    val extensions = gpxWaypoint.extensions

                    Waypoint(
                        name = gpxWaypoint.name.orElse("Imported Waypoint"),
                        latitude = gpxWaypoint.latitude.toDouble(),
                        longitude = gpxWaypoint.longitude.toDouble(),
                        altitude = gpxWaypoint.elevation
                            .map { it.toDouble() }
                            .orElse(0.0),
                        color = extensions["color"]
                            ?.let { Waypoint.WaypointColor.fromName(it) }
                            ?: Waypoint.WaypointColor.RED,
                        description = gpxWaypoint.description.orElse(""),
                        iconType = extensions["icon"]
                            ?.let { Waypoint.IconType.fromName(it) }
                            ?: Waypoint.IconType.PIN,
                        createdAt = gpxWaypoint.time
                            .map { it.toInstant().toEpochMilli() }
                            .orElse(System.currentTimeMillis())
                    )
                }.toList()

                Result.success(waypoints)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Get list of exported GPX files
     */
    suspend fun getExportedFiles(): List<File> = withContext(Dispatchers.IO) {
        val exportDir = File(context.getExternalFilesDir(null), GPX_EXPORT_DIR)
        if (exportDir.exists() && exportDir.isDirectory) {
            exportDir.listFiles { file ->
                file.extension.equals("gpx", ignoreCase = true)
            }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    /**
     * Delete exported file
     */
    suspend fun deleteExportedFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        val exportDir = File(context.getExternalFilesDir(null), GPX_EXPORT_DIR)
        val file = File(exportDir, fileName)
        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    /**
     * Share GPX file
     */
    fun getShareUri(file: File): Uri? {
        return try {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            null
        }
    }
}