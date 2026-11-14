
package com.yash.fitnesstracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.yash.fitnesstracker.MainActivity
import com.yash.fitnesstracker.R
import com.yash.fitnesstracker.repository.StepRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ForegroundService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val checkpointMutex = Mutex()

    private var currentDate = ""
    private var dailySteps = 0
    private var lastSensorSteps = -1
    private var lastCheckpointTime = 0L
    private var initialized = false

    private val checkpointIntervalMs = 30_000L

    override fun onCreate() {
        super.onCreate()

        StepRepository.initialize(applicationContext)
        DataStoreManager.init(applicationContext)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Log.e("ForegroundService", "Step counter sensor unavailable")
            stopSelf()
            return
        }

        serviceScope.launch {
            restoreCheckpoint()
            startCheckpointLoop()
        }

        sensorManager.registerListener(
            this,
            stepSensor,
            SensorManager.SENSOR_DELAY_UI
        )

        Log.d("ForegroundService", "Step sensor registered")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(
            this,
            "FitnessTrackerChannel"
        )
            .setContentTitle("Steps Counter")
            .setContentText("Tracking your steps")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        if (dailySteps > 0) {
            StepRepository.updateSteps(dailySteps)
        } else {
            serviceScope.launch {
                restoreCheckpoint()
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "FitnessTrackerChannel",
                "Step Counter Service",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    private suspend fun restoreCheckpoint() {
        checkpointMutex.withLock {
            val today = todayString()
            val checkpoint = DataStoreManager.getStepCheckpoint(
                applicationContext
            )

            if (checkpoint.third == today) {
                dailySteps = checkpoint.first.coerceAtLeast(0)
                lastSensorSteps = checkpoint.second
            } else {
                dailySteps = 0
                lastSensorSteps = -1
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val roomEntity = StepRepository.getStepsForDate(today)
                    if (roomEntity != null && roomEntity.steps > dailySteps) {
                        dailySteps = roomEntity.steps
                    }
                }
            } catch (_: Exception) {}

            currentDate = today
            initialized = true

            StepRepository.updateSteps(dailySteps)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                StepRepository.insertOrUpdateInRoom(dailySteps)
            }

            Log.d(
                "StepRecovery",
                "Restored dailySteps=$dailySteps, lastSensorSteps=$lastSensorSteps"
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_STEP_COUNTER) {
            return
        }

        val sensorSteps = event.values[0].toLong()
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

        serviceScope.launch {
            checkpointMutex.withLock {
                if (!initialized) return@withLock

                val today = todayString()

                if (today != currentDate) {
                    currentDate = today
                    dailySteps = 0
                    lastSensorSteps = sensorSteps

                    saveCheckpoint(sensorSteps)
                    return@withLock
                }

                if (lastSensorSteps < 0) {
                    // First reading after startup/reboot:
                    // retain the restored daily total and establish a new baseline.
                    lastSensorSteps = sensorSteps
                } else if (sensorSteps >= lastSensorSteps) {
                    dailySteps += sensorSteps - lastSensorSteps
                    lastSensorSteps = sensorSteps
                } else {
                    // Sensor counter reset or restarted.
                    // Do not discard the steps already saved for today.
                    lastSensorSteps = sensorSteps
                }

                StepRepository.updateSteps(dailySteps)

                val now = System.currentTimeMillis()
                if (now - lastCheckpointTime >= checkpointIntervalMs) {
                    saveCheckpoint(sensorSteps)
                }
            }
        }
    }

    private suspend fun startCheckpointLoop() {
        while (true) {
            delay(checkpointIntervalMs)

            checkpointMutex.withLock {
                if (!initialized) return@withLock

                val today = todayString()

                if (today != currentDate) {
                    currentDate = today
                    dailySteps = 0
                    lastSensorSteps = -1
                    StepRepository.updateSteps(0)
                }

                saveCheckpoint(lastSensorSteps)
            }
        }
    }

    private suspend fun saveCheckpoint(sensorSteps: Int) {
        val today = todayString()

        DataStoreManager.saveStepCheckpoint(
            context = applicationContext,
            dailySteps = dailySteps,
            sensorSteps = sensorSteps,
            date = today
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            StepRepository.insertOrUpdateInRoom(dailySteps)
        }
        lastCheckpointTime = System.currentTimeMillis()

        Log.d(
            "StepCheckpoint",
            "Saved $dailySteps steps for $today"
        )
    }

    private fun todayString(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        } else {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
        super.onDestroy()
    }
}
