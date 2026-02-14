package com.navigationpro.di

import android.content.Context
import com.navigationpro.data.dao.WaypointDao
import com.navigationpro.data.database.NavigationDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Application-level dependency injection module
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provide Navigation Database
     */
    @Provides
    @Singleton
    fun provideNavigationDatabase(
        @ApplicationContext context: Context
    ): NavigationDatabase {
        return NavigationDatabase.getInstance(context)
    }

    /**
     * Provide WaypointDao
     */
    @Provides
    @Singleton
    fun provideWaypointDao(
        database: NavigationDatabase
    ): WaypointDao {
        return database.waypointDao()
    }
}