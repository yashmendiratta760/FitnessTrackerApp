package com.yash.fitnesstracker.repository

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.yash.fitnesstracker.MainApplication
import com.yash.fitnesstracker.database.StepsEntities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object StepRepository {

    private lateinit var appContainer: AppContainer

    fun initialize(context: Context) {
        appContainer = (context.applicationContext as MainApplication).container
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            loadTodaySteps()
        }
    }

    private val stepsLocalDbRepository
        get() = appContainer.stepsLocalDbRepository

    private val _time = MutableStateFlow(0L)
    val timeCount: StateFlow<Long> = _time

    fun updateTime(time: Long) {
        _time.value = time
    }

    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount

    fun updateSteps(steps: Int) {
        if (steps >= 0) {
            _stepCount.value = steps
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadTodaySteps() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val date = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                val existingEntity = stepsLocalDbRepository.getStepsByDate(date)
                if (existingEntity != null && existingEntity.steps > _stepCount.value) {
                    _stepCount.value = existingEntity.steps
                }
            } catch (e: Exception) {
                Log.e("StepRepository", "Error restoring initial steps from Room", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getStepsForDate(date: String): StepsEntities? {
        return try {
            stepsLocalDbRepository.getStepsByDate(date)
        } catch (_: Exception) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun insertOrUpdateInRoom(steps: Int) {
        val date = LocalDate.now()
        val formattedDate = date.format(DateTimeFormatter.ISO_DATE)

        val existingEntity = stepsLocalDbRepository.getStepsByDate(formattedDate)
        if (existingEntity != null) {
            val updatedEntity = existingEntity.copy(steps = steps)
            stepsLocalDbRepository.update(updatedEntity)
        } else {
            val newEntity = StepsEntities(steps = steps, date = formattedDate)
            stepsLocalDbRepository.insert(newEntity)
        }
    }
}
