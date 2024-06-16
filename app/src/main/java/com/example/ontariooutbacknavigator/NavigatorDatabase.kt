package com.example.ontariooutbacknavigator

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Waypoint::class], version = 1)
abstract class NavigatorDatabase : RoomDatabase() {
    abstract fun waypointDao(): WaypointDao
}