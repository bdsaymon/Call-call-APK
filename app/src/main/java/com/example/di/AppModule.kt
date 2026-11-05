package com.example.di

import android.content.Context
import com.example.R
import com.example.audio.TactileAudioEngine
import com.example.data.local.LocalAliasDao
import com.example.data.local.NeoTactileDatabase
import com.example.data.repository.NeoTactileRepository
import com.example.security.DeviceIdentityManager
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Dependency Injection Provider Module for Neo-Tactile Architecture:
 * Provides Database (Room), Firebase (Firestore with custom database ID), and Audio/Security managers.
 */
object AppModule {

    @Volatile
    private var audioEngineInstance: TactileAudioEngine? = null

    @Volatile
    private var deviceIdentityInstance: DeviceIdentityManager? = null

    fun provideFirestore(context: Context): FirebaseFirestore {
        val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
        return FirebaseFirestore.getInstance(databaseId)
    }

    fun provideRoomDatabase(context: Context): NeoTactileDatabase {
        return NeoTactileDatabase.getInstance(context.applicationContext)
    }

    fun provideLocalAliasDao(context: Context): LocalAliasDao {
        return provideRoomDatabase(context).localAliasDao()
    }

    fun provideRepository(context: Context): NeoTactileRepository {
        return NeoTactileRepository(
            db = provideFirestore(context),
            localAliasDao = provideLocalAliasDao(context)
        )
    }

    fun provideAudioEngine(context: Context): TactileAudioEngine {
        return audioEngineInstance ?: synchronized(this) {
            audioEngineInstance ?: TactileAudioEngine(context.applicationContext).also {
                audioEngineInstance = it
            }
        }
    }

    fun provideDeviceIdentityManager(context: Context): DeviceIdentityManager {
        return deviceIdentityInstance ?: synchronized(this) {
            deviceIdentityInstance ?: DeviceIdentityManager(context.applicationContext).also {
                deviceIdentityInstance = it
            }
        }
    }
}
