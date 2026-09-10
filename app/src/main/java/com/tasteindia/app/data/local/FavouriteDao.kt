package com.tasteindia.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favourite: FavouriteEntity)

    @Query("DELETE FROM favourites WHERE mealId = :mealId")
    suspend fun delete(mealId: String)

    @Query("SELECT mealId FROM favourites")
    fun getAllIds(): Flow<List<String>>
}
