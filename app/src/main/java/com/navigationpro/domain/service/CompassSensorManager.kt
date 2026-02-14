package com.navigationpro.domain.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Compass Sensor Manager with low-pass filtering
 * Provides smooth azimuth readings from magnetometer and accelerometer fusion
 */
@Singleton
class CompassSensorManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Sensors
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val rotationVector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    // Low-pass filter constant (0.0 - 1.0, higher = more smoothing)
    private val alpha = 0.15f

    // Sensor data arrays
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    // Filtered values
    private val filteredAccelerometer = FloatArray(3)
    private val filteredMagnetometer = FloatArray(3)

    // Rotation matrix and orientation
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    /**
     * Compass data class containing azimuth and accuracy
     */
    data class CompassData(
        val azimuth: Float,        // 0-360 degrees, 0 = North
        val pitch: Float,          // Device tilt
        val roll: Float,           // Device roll
        val accuracy: Int,         // Sensor accuracy (SENSOR_STATUS_*)
        val hasRotationVector: Boolean // Whether using rotation vector sensor
    ) {
        /**
         * Get cardinal direction string
         */
        fun getCardinalDirection(): String {
            return when {
                azimuth >= 337.5f || azimuth < 22.5f -> "N"
                azimuth >= 22.5f && azimuth < 67.5f -> "NE"
                azimuth >= 67.5f && azimuth < 112.5f -> "E"
                azimuth >= 112.5f && azimuth < 157.5f -> "SE"
                azimuth >= 157.5f && azimuth < 202.5f -> "S"
                azimuth >= 202.5f && azimuth < 247.5f -> "SW"
                azimuth >= 247.5f && azimuth < 292.5f -> "W"
                azimuth >= 292.5f && azimuth < 337.5f -> "NW"
                else -> "N"
            }
        }

        /**
         * Get full direction name
         */
        fun getDirectionName(): String {
            return when {
                azimuth >= 337.5f || azimuth < 22.5f -> "NORTH"
                azimuth >= 22.5f && azimuth < 67.5f -> "NORTHEAST"
                azimuth >= 67.5f && azimuth < 112.5f -> "EAST"
                azimuth >= 112.5f && azimuth < 157.5f -> "SOUTHEAST"
                azimuth >= 157.5f && azimuth < 202.5f -> "SOUTH"
                azimuth >= 202.5f && azimuth < 247.5f -> "SOUTHWEST"
                azimuth >= 247.5f && azimuth < 292.5f -> "WEST"
                azimuth >= 292.5f && azimuth < 337.5f -> "NORTHWEST"
                else -> "NORTH"
            }
        }
    }

    /**
     * Flow of compass data with low-pass filtering
     */
    fun getCompassDataFlow(): Flow<CompassData> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        handleRotationVector(event)
                        // Emit updated compass data
                        val data = calculateCompassData()
                        trySend(data)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        // Apply low-pass filter
                        accelerometerReading[0] = event.values[0]
                        accelerometerReading[1] = event.values[1]
                        accelerometerReading[2] = event.values[2]

                        filteredAccelerometer[0] = lowPass(
                            accelerometerReading[0],
                            filteredAccelerometer[0]
                        )
                        filteredAccelerometer[1] = lowPass(
                            accelerometerReading[1],
                            filteredAccelerometer[1]
                        )
                        filteredAccelerometer[2] = lowPass(
                            accelerometerReading[2],
                            filteredAccelerometer[2]
                        )
                        
                        // Calculate and emit if magnetometer data is available
                        if (filteredMagnetometer[0] != 0f || filteredMagnetometer[1] != 0f) {
                            calculateOrientation()
                            val data = calculateCompassData()
                            trySend(data)
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        // Apply low-pass filter
                        magnetometerReading[0] = event.values[0]
                        magnetometerReading[1] = event.values[1]
                        magnetometerReading[2] = event.values[2]

                        filteredMagnetometer[0] = lowPass(
                            magnetometerReading[0],
                            filteredMagnetometer[0]
                        )
                        filteredMagnetometer[1] = lowPass(
                            magnetometerReading[1],
                            filteredMagnetometer[1]
                        )
                        filteredMagnetometer[2] = lowPass(
                            magnetometerReading[2],
                            filteredMagnetometer[2]
                        )

                        // Calculate orientation from accelerometer + magnetometer
                        calculateOrientation()
                        val data = calculateCompassData()
                        trySend(data)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                // Handle accuracy changes if needed
            }
        }

        // Register rotation vector sensor if available (more accurate)
        rotationVector?.let {
            sensorManager.registerListener(
                listener,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        } ?: run {
            // Fall back to accelerometer + magnetometer
            accelerometer?.let {
                sensorManager.registerListener(
                    listener,
                    it,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
            magnetometer?.let {
                sensorManager.registerListener(
                    listener,
                    it,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
        .distinctUntilChanged { old, new ->
            abs(old.azimuth - new.azimuth) < 1.0f // Only emit when change > 1 degree
        }

    /**
     * Handle rotation vector sensor data
     */
    private fun handleRotationVector(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
    }

    /**
     * Calculate orientation from accelerometer and magnetometer
     */
    private fun calculateOrientation() {
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            filteredAccelerometer,
            filteredMagnetometer
        )

        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
        }
    }

    /**
     * Calculate compass data from current sensor readings
     */
    private fun calculateCompassData(): CompassData {
        // Get azimuth in radians and convert to degrees
        var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

        // Adjust for screen rotation
        azimuth = adjustForScreenRotation(azimuth)

        // Normalize to 0-360
        azimuth = (azimuth + 360) % 360

        val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

        return CompassData(
            azimuth = azimuth,
            pitch = pitch,
            roll = roll,
            accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
            hasRotationVector = rotationVector != null
        )
    }

    /**
     * Adjust azimuth based on screen rotation
     */
    private fun adjustForScreenRotation(azimuth: Float): Float {
        val rotation = windowManager.defaultDisplay.rotation
        return when (rotation) {
            Surface.ROTATION_0 -> azimuth
            Surface.ROTATION_90 -> azimuth + 90
            Surface.ROTATION_180 -> azimuth + 180
            Surface.ROTATION_270 -> azimuth + 270
            else -> azimuth
        }
    }

    /**
     * Low-pass filter to smooth sensor readings
     */
    private fun lowPass(input: Float, output: Float): Float {
        return output + alpha * (input - output)
    }

    /**
     * Check if compass sensors are available
     */
    fun hasCompass(): Boolean {
        return rotationVector != null ||
               (accelerometer != null && magnetometer != null)
    }

    /**
     * Get sensor accuracy description
     */
    fun getAccuracyDescription(accuracy: Int): String {
        return when (accuracy) {
            SensorManager.SENSOR_STATUS_UNRELIABLE -> "UNRELIABLE"
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "LOW"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "MEDIUM"
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "HIGH"
            else -> "UNKNOWN"
        }
    }

    /**
     * Calibrate compass (trigger re-calibration)
     */
    fun calibrate() {
        // Reset filtered values
        filteredAccelerometer.fill(0f)
        filteredMagnetometer.fill(0f)
    }
}