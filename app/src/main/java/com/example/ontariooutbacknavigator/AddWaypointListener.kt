package com.example.ontariooutbacknavigator

interface AddWaypointListener {
    fun addWaypoint(lat : Double, lng : Double, description : String, color : Int)
}