package com.Chenkham.Echofy.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import timber.log.Timber

class FlipToPauseDetector(
    private val context: Context,
    private val onFaceDown: () -> Unit,
    private val onFaceUp: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val proximity = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private var isFaceDown = false
    private var isProximityNear = false
    private var isListening = false

    fun start() {
        if (isListening || sensorManager == null) return
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        proximity?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        isListening = true
        Timber.d("FlipToPauseDetector started")
    }

    fun stop() {
        if (!isListening || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isListening = false
        isFaceDown = false
        Timber.d("FlipToPauseDetector stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values[0]
                val maxRange = event.sensor.maximumRange
                isProximityNear = distance < maxRange.coerceAtMost(5.0f)
                checkOrientation(0f)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val z = event.values[2]
                checkOrientation(z)
            }
        }
    }

    private fun checkOrientation(z: Float) {
        // Face down: Z is negative (pointing down) and device is on surface / proximity near
        if (z < -7.5f) {
            if (!isFaceDown) {
                isFaceDown = true
                Timber.d("FlipToPause: Device placed face-down -> pausing")
                onFaceDown()
            }
        } else if (z > 4.5f) {
            if (isFaceDown) {
                isFaceDown = false
                Timber.d("FlipToPause: Device picked face-up -> resuming")
                onFaceUp()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
