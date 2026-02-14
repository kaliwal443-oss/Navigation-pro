package com.navigationpro

import androidx.multidex.MultiDexApplication
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import java.io.File

/**
 * Application class for Navigation Pro
 * Initializes osmdroid and Hilt dependency injection
 */
@HiltAndroidApp
class NavigationProApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        initializeOsmdroid()
    }

    private fun initializeOsmdroid() {
        val osmConfig = Configuration.getInstance()
        
        osmConfig.userAgentValue = packageName
        
        val tileCache = File(cacheDir, "osmdroid")
        if (!tileCache.exists()) {
            tileCache.mkdirs()
        }
        osmConfig.osmdroidBasePath = tileCache
        osmConfig.osmdroidTileCache = File(tileCache, "tiles")
        
        osmConfig.cacheMapTileCount = 200
        osmConfig.tileDownloadThreads = 4
        osmConfig.tileFileSystemThreads = 4
        osmConfig.expirationAddDuration = 7 * 24 * 60 * 60 * 1000L
        
        osmConfig.isDebugMode = BuildConfig.DEBUG
        osmConfig.isDebugMapView = BuildConfig.DEBUG
    }
}