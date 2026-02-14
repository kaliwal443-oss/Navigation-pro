package com.navigationpro.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity - Entry point for Navigation Pro
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Approximate location access granted
            }
            else -> {
                // No location access granted
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure window for edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Hide system bars for immersive experience
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Check and request location permissions
        checkLocationPermissions()

        setContent {
            NavigationProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MapScreen()
                }
            }
        }
    }

    /**
     * Check and request location permissions
     */
    private fun checkLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Show rationale UI
                requestLocationPermissions()
            }
            else -> {
                // Request permission
                requestLocationPermissions()
            }
        }
    }

    /**
     * Request location permissions
     */
    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}

/**
 * Navigation Pro Theme
 */
@Composable
fun NavigationProTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF10b981),
            onPrimary = androidx.compose.ui.graphics.Color.White,
            primaryContainer = androidx.compose.ui.graphics.Color(0xFF059669),
            onPrimaryContainer = androidx.compose.ui.graphics.Color.White,
            secondary = androidx.compose.ui.graphics.Color(0xFFf59e0b),
            onSecondary = androidx.compose.ui.graphics.Color.Black,
            secondaryContainer = androidx.compose.ui.graphics.Color(0xFFd97706),
            onSecondaryContainer = androidx.compose.ui.graphics.Color.Black,
            background = androidx.compose.ui.graphics.Color(0xFF0f172a),
            onBackground = androidx.compose.ui.graphics.Color.White,
            surface = androidx.compose.ui.graphics.Color(0xFF1e293b),
            onSurface = androidx.compose.ui.graphics.Color.White,
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFF334155),
            onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF94a3b8),
            error = androidx.compose.ui.graphics.Color(0xFFef4444),
            onError = androidx.compose.ui.graphics.Color.White,
            outline = androidx.compose.ui.graphics.Color(0xFF475569)
        ),
        typography = androidx.compose.material3.Typography(
            // Use default typography with monospace for data
        ),
        content = content
    )
}