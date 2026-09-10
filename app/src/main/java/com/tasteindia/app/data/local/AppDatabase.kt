package com.tasteindia.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FavouriteEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favouriteDao(): FavouriteDao
}
