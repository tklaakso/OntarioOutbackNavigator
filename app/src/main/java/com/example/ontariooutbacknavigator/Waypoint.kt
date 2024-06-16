package com.example.ontariooutbacknavigator

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "waypoints")
data class Waypoint (
    @PrimaryKey(autoGenerate = true) var id: Int,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "color") val color: Int
)