package com.example.ontariooutbacknavigator

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface WaypointDao {
    @Query("SELECT * FROM waypoints")
    suspend fun getAll(): List<Waypoint>

    @Query("SELECT * FROM waypoints WHERE id = :id")
    suspend fun getById(id: Int): Waypoint?

    @Update
    suspend fun update(waypoint: Waypoint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(waypoint: Waypoint) : Long

    @Delete
    suspend fun delete(waypoint: Waypoint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(waypoints: List<Waypoint>)

    @Query("DELETE FROM waypoints")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(waypoints: List<Waypoint>) {
        deleteAll()
        insertAll(waypoints)
    }
}