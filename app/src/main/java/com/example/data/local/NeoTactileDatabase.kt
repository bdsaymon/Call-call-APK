package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LocalAliasEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NeoTactileDatabase : RoomDatabase() {
    abstract fun localAliasDao(): LocalAliasDao

    companion object {
        @Volatile
        private var INSTANCE: NeoTactileDatabase? = null

        fun getInstance(context: Context): NeoTactileDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NeoTactileDatabase::class.java,
                    "neotactile_local_overrides.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
