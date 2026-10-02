package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SavedPinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPinDao {

    @Query("SELECT * FROM saved_pins ORDER BY savedAt DESC")
    fun getAllSavedPins(): Flow<List<SavedPinEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_pins WHERE id = :pinId)")
    fun isPinSaved(pinId: String): Flow<Boolean>

    @Query("SELECT * FROM saved_pins WHERE id = :pinId LIMIT 1")
    suspend fun getPinById(pinId: String): SavedPinEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePin(pin: SavedPinEntity)

    @Query("DELETE FROM saved_pins WHERE id = :pinId")
    suspend fun deletePinById(pinId: String)
}
