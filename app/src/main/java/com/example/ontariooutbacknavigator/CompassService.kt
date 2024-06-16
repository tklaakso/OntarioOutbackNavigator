package com.example.ontariooutbacknavigator

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat.getSystemService

class CompassService(context : Context, private val listener : CompassEventListener) : SensorEventListener {

    private var mGravity: FloatArray? = null
    private var mGeomagnetic: FloatArray? = null

    private var averageAzimuth = 0f

    init {
        val sensorManager = getSystemService(context, SensorManager::class.java) as SensorManager
        val magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) mGravity = event.values
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) mGeomagnetic = event.values
        if (mGravity != null && mGeomagnetic != null) {
            val R = FloatArray(9)
            val I = FloatArray(9)
            val success = SensorManager.getRotationMatrix(
                R, I, mGravity,
                mGeomagnetic
            )
            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(R, orientation)
                val azimuth = -orientation[0]
                if (azimuth - averageAzimuth > Math.PI) {
                    averageAzimuth += Math.PI.toFloat() * 2
                } else if (azimuth - averageAzimuth < -Math.PI) {
                    averageAzimuth -= Math.PI.toFloat() * 2
                }
                averageAzimuth = (4 * averageAzimuth + azimuth) / 5
                listener.onCompassChanged(averageAzimuth)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}