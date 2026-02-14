package com.navigationpro.presentation.components

import android.content.Context
import android.graphics.*
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.util.GeoPoint
import com.navigationpro.domain.util.ProjectionMath
import kotlin.math.*

/**
 * Custom Overlay that draws Indian Grid System grid lines on the map
 * Dynamically renders grid lines based on current map bounding box
 */
class IndianGridOverlay(context: Context) : Overlay() {

    // Paint objects for drawing
    private val gridLinePaint = Paint().apply {
        color = Color.parseColor("#334155") // Slate-700
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val gridLabelPaint = Paint().apply {
        color = Color.parseColor("#94a3b8") // Slate-400
        textSize = 24f
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
    }

    private val zoneBoundaryPaint = Paint().apply {
        color = Color.parseColor("#f59e0b") // Amber
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
        pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
    }

    private val labelBackgroundPaint = Paint().apply {
        color = Color.parseColor("#CC0f172a") // Semi-transparent slate-950
        style = Paint.Style.FILL
    }

    // Grid interval in meters (increases with zoom level)
    private var gridInterval = 10000.0 // 10km default

    // Show/hide grid
    var isGridVisible = true
        set(value) {
            field = value
            invalidate()
        }

    // Show zone boundaries
    var showZoneBoundaries = true
        set(value) {
            field = value
            invalidate()
        }

    /**
     * Set grid line color
     */
    fun setGridColor(color: Int) {
        gridLinePaint.color = color
    }

    /**
     * Set grid label color
     */
    fun setLabelColor(color: Int) {
        gridLabelPaint.color = color
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || !isGridVisible) return

        val projection = mapView.projection
        val zoomLevel = mapView.zoomLevelDouble

        // Adjust grid interval based on zoom level
        gridInterval = calculateGridInterval(zoomLevel)

        // Get visible map bounds
        val bounds = mapView.boundingBox
        val minLat = bounds.latSouth
        val maxLat = bounds.latNorth
        val minLng = bounds.lonWest
        val maxLng = bounds.lonEast

        // Find which zones are visible
        val visibleZones = ProjectionMath.GridZone.values().filter { zone ->
            // Check if zone overlaps with visible bounds
            zoneOverlapsBounds(zone, minLat, maxLat, minLng, maxLng)
        }

        if (visibleZones.isEmpty()) return

        // Draw zone boundaries
        if (showZoneBoundaries) {
            visibleZones.forEach { zone ->
                drawZoneBoundary(canvas, projection, zone)
            }
        }

        // Draw grid lines for each visible zone
        visibleZones.forEach { zone ->
            drawGridLines(canvas, projection, zone, minLat, maxLat, minLng, maxLng)
        }
    }

    /**
     * Calculate appropriate grid interval based on zoom level
     */
    private fun calculateGridInterval(zoomLevel: Double): Double {
        return when {
            zoomLevel >= 18 -> 100.0      // 100m
            zoomLevel >= 16 -> 500.0      // 500m
            zoomLevel >= 14 -> 1000.0     // 1km
            zoomLevel >= 12 -> 5000.0     // 5km
            zoomLevel >= 10 -> 10000.0    // 10km
            zoomLevel >= 8 -> 50000.0     // 50km
            zoomLevel >= 6 -> 100000.0    // 100km
            else -> 250000.0              // 250km
        }
    }

    /**
     * Check if a zone overlaps with the visible bounds
     */
    private fun zoneOverlapsBounds(
        zone: ProjectionMath.GridZone,
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): Boolean {
        return !(zone.maxLat < minLat || zone.minLat > maxLat ||
                 zone.maxLng < minLng || zone.minLng > maxLng)
    }

    /**
     * Draw zone boundary
     */
    private fun drawZoneBoundary(
        canvas: Canvas,
        projection: org.osmdroid.views.Projection,
        zone: ProjectionMath.GridZone
    ) {
        val path = Path()

        // Zone corners
        val corners = listOf(
            GeoPoint(zone.maxLat, zone.minLng),
            GeoPoint(zone.maxLat, zone.maxLng),
            GeoPoint(zone.minLat, zone.maxLng),
            GeoPoint(zone.minLat, zone.minLng)
        )

        // Convert to screen coordinates
        val screenPoints = corners.map { geoPoint ->
            val point = Point()
            projection.toPixels(geoPoint, point)
            point
        }

        // Draw boundary path
        path.moveTo(screenPoints[0].x.toFloat(), screenPoints[0].y.toFloat())
        for (i in 1 until screenPoints.size) {
            path.lineTo(screenPoints[i].x.toFloat(), screenPoints[i].y.toFloat())
        }
        path.close()

        canvas.drawPath(path, zoneBoundaryPaint)

        // Draw zone label
        val centerPoint = Point()
        val centerGeo = GeoPoint(
            (zone.minLat + zone.maxLat) / 2,
            (zone.minLng + zone.maxLng) / 2
        )
        projection.toPixels(centerGeo, centerPoint)

        val label = zone.zoneName
        val labelWidth = gridLabelPaint.measureText(label)
        val labelHeight = gridLabelPaint.fontMetrics.descent - gridLabelPaint.fontMetrics.ascent

        // Draw label background
        canvas.drawRect(
            centerPoint.x - labelWidth / 2 - 8,
            centerPoint.y - labelHeight / 2 - 4,
            centerPoint.x + labelWidth / 2 + 8,
            centerPoint.y + labelHeight / 2 + 4,
            labelBackgroundPaint
        )

        // Draw label text
        canvas.drawText(
            label,
            centerPoint.x - labelWidth / 2,
            centerPoint.y + labelHeight / 4,
            gridLabelPaint
        )
    }

    /**
     * Draw grid lines within a zone
     */
    private fun drawGridLines(
        canvas: Canvas,
        projection: org.osmdroid.views.Projection,
        zone: ProjectionMath.GridZone,
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ) {
        // Calculate grid bounds in Indian Grid coordinates
        val sw = ProjectionMath.wgs84ToIndianGrid(minLat, minLng, zone)
        val ne = ProjectionMath.wgs84ToIndianGrid(maxLat, maxLng, zone)

        if (sw == null || ne == null) return

        // Round to nearest grid interval
        val minEasting = floor(sw.easting / gridInterval) * gridInterval
        val maxEasting = ceil(ne.easting / gridInterval) * gridInterval
        val minNorthing = floor(sw.northing / gridInterval) * gridInterval
        val maxNorthing = ceil(ne.northing / gridInterval) * gridInterval

        // Draw easting lines (vertical)
        var easting = minEasting
        while (easting <= maxEasting) {
            drawEastingLine(canvas, projection, zone, easting, minNorthing, maxNorthing)
            easting += gridInterval
        }

        // Draw northing lines (horizontal)
        var northing = minNorthing
        while (northing <= maxNorthing) {
            drawNorthingLine(canvas, projection, zone, northing, minEasting, maxEasting)
            northing += gridInterval
        }
    }

    /**
     * Draw a vertical (easting) grid line
     */
    private fun drawEastingLine(
        canvas: Canvas,
        projection: org.osmdroid.views.Projection,
        zone: ProjectionMath.GridZone,
        easting: Double,
        minNorthing: Double,
        maxNorthing: Double
    ) {
        val path = Path()
        var firstPoint = true

        // Sample points along the line
        val steps = 20
        val northingStep = (maxNorthing - minNorthing) / steps

        for (i in 0..steps) {
            val northing = minNorthing + i * northingStep
            val wgs84 = ProjectionMath.indianGridToWgs84(easting, northing, zone)

            if (wgs84 != null) {
                val point = Point()
                projection.toPixels(GeoPoint(wgs84.first, wgs84.second), point)

                if (firstPoint) {
                    path.moveTo(point.x.toFloat(), point.y.toFloat())
                    firstPoint = false
                } else {
                    path.lineTo(point.x.toFloat(), point.y.toFloat())
                }
            }
        }

        canvas.drawPath(path, gridLinePaint)

        // Draw label at top of line
        val topWgs84 = ProjectionMath.indianGridToWgs84(easting, maxNorthing, zone)
        if (topWgs84 != null) {
            val labelPoint = Point()
            projection.toPixels(GeoPoint(topWgs84.first, topWgs84.second), labelPoint)

            val label = "${(easting / 1000).toInt()}"
            val labelWidth = gridLabelPaint.measureText(label)

            canvas.drawRect(
                labelPoint.x - labelWidth / 2 - 4,
                labelPoint.y - 30f,
                labelPoint.x + labelWidth / 2 + 4,
                labelPoint.y - 6f,
                labelBackgroundPaint
            )

            canvas.drawText(
                label,
                labelPoint.x - labelWidth / 2,
                labelPoint.y - 10f,
                gridLabelPaint
            )
        }
    }

    /**
     * Draw a horizontal (northing) grid line
     */
    private fun drawNorthingLine(
        canvas: Canvas,
        projection: org.osmdroid.views.Projection,
        zone: ProjectionMath.GridZone,
        northing: Double,
        minEasting: Double,
        maxEasting: Double
    ) {
        val path = Path()
        var firstPoint = true

        // Sample points along the line
        val steps = 20
        val eastingStep = (maxEasting - minEasting) / steps

        for (i in 0..steps) {
            val easting = minEasting + i * eastingStep
            val wgs84 = ProjectionMath.indianGridToWgs84(easting, northing, zone)

            if (wgs84 != null) {
                val point = Point()
                projection.toPixels(GeoPoint(wgs84.first, wgs84.second), point)

                if (firstPoint) {
                    path.moveTo(point.x.toFloat(), point.y.toFloat())
                    firstPoint = false
                } else {
                    path.lineTo(point.x.toFloat(), point.y.toFloat())
                }
            }
        }

        canvas.drawPath(path, gridLinePaint)

        // Draw label at left of line
        val leftWgs84 = ProjectionMath.indianGridToWgs84(minEasting, northing, zone)
        if (leftWgs84 != null) {
            val labelPoint = Point()
            projection.toPixels(GeoPoint(leftWgs84.first, leftWgs84.second), labelPoint)

            val label = "${(northing / 1000).toInt()}"

            canvas.drawRect(
                labelPoint.x - gridLabelPaint.measureText(label) - 8,
                labelPoint.y - 12,
                labelPoint.x - 4,
                labelPoint.y + 12,
                labelBackgroundPaint
            )

            canvas.drawText(
                label,
                labelPoint.x - gridLabelPaint.measureText(label) - 4,
                labelPoint.y + 6,
                gridLabelPaint
            )
        }
    }
}