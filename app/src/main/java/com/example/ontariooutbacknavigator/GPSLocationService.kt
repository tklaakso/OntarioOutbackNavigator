package com.example.ontariooutbacknavigator

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService

class GPSLocationService(private val activity: Activity, private val locationListener: LocationListener) {

    private lateinit var locationManager: LocationManager

    init {
        initLocationManager()
    }

    @SuppressLint("MissingPermission")
    private fun initLocationManager() {
        locationManager = getSystemService(activity.applicationContext, LocationManager::class.java) as LocationManager
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
    }
}