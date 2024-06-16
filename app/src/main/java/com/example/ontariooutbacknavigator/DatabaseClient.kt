package com.example.ontariooutbacknavigator

import android.content.Context
import androidx.room.Room

class DatabaseClient(context: Context) {

    private var db : NavigatorDatabase? = null

    init {
        db = Room.databaseBuilder(
            context,
            NavigatorDatabase::class.java, "navigator-database"
        ).build()
    }

    fun getDB() : NavigatorDatabase {
        return db!!
    }

}