package com.yash.fitnesstracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yash.fitnesstracker.MainApplication
import com.yash.fitnesstracker.api.ExerciseWorkout
import com.yash.fitnesstracker.api.PlannedExercise
import com.yash.fitnesstracker.api.WorkoutSession
import com.yash.fitnesstracker.api.WorkoutSet
import com.yash.fitnesstracker.repository.PlannerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PlannerViewModel(
    private val repository: PlannerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PlannerUiState(
            selectedDay = currentDayAbbreviation()
        )
    )
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init {
        // 1. Observe Room DB weekly plans flow
        viewModelScope.launch {
            repository.weeklyPlansFlow.collectLatest { plansMap ->
                _uiState.update { current ->
                    current.copy(plans = plansMap)
                }
            }
        }

        // 2. Observe Room DB workout history flow
        viewModelScope.launch {
            repository.workoutHistoryFlow.collectLatest { historyList ->
                _uiState.update { current ->
                    val today = todayDateString()
                    val matchingSession = historyList.firstOrNull {
                        it.date == today && it.day == current.selectedDay
                    }
                    current.copy(
                        history = historyList,
                        currentSession = current.currentSession ?: matchingSession
                    )
                }
            }
        }

        // 3. Trigger initial sync with backend
        syncWithBackend()
    }

    fun selectDay(day: String) {
        val today = todayDateString()
        val matchingSession = _uiState.value.history.firstOrNull {
            it.date == today && it.day == day
        }
        _uiState.update {
            it.copy(
                selectedDay = day,
                currentSession = matchingSession
            )
        }
    }

    fun syncWithBackend() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, isLoading = it.plans.isEmpty()) }

            val isHealthy = repository.checkBackendHealth()
            _uiState.update { it.copy(isBackendHealthy = isHealthy) }

            val plansResult = repository.syncWeeklyPlans()
            val historyResult = repository.syncWorkoutHistory()

            val errorMessage = when {
                plansResult.isFailure && historyResult.isFailure ->
                    "Unable to connect to backend. Working offline."
                plansResult.isFailure ->
                    "Failed to sync plans from backend. Offline plans loaded."
                historyResult.isFailure ->
                    "Failed to sync workout history from backend. Offline history loaded."
                else -> null
            }

            _uiState.update {
                it.copy(
                    isSyncing = false,
                    isLoading = false,
                    errorMessage = errorMessage
                )
            }
        }
    }

    fun addOrUpdateExercise(day: String, exercise: PlannedExercise) {
        viewModelScope.launch {
            val currentExercises = _uiState.value.plans[day] ?: emptyList()
            val existingIndex = currentExercises.indexOfFirst { it.id == exercise.id }
            val updated = if (existingIndex >= 0) {
                currentExercises.toMutableList().apply { set(existingIndex, exercise) }
            } else {
                currentExercises + exercise
            }

            // Save to Room DB and sync to backend
            val result = repository.saveDayPlan(day, updated)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(errorMessage = "Exercise saved offline. Backend sync pending.")
                }
            }
        }
    }

    fun deleteExercise(day: String, exerciseId: String) {
        viewModelScope.launch {
            val currentExercises = _uiState.value.plans[day] ?: emptyList()
            val updated = currentExercises.filterNot { it.id == exerciseId }

            val result = if (updated.isEmpty()) {
                repository.deleteDayPlan(day).map { emptyList<PlannedExercise>() }
            } else {
                repository.saveDayPlan(day, updated)
            }

            if (result.isFailure) {
                _uiState.update {
                    it.copy(errorMessage = "Exercise deleted offline. Backend sync pending.")
                }
            }
        }
    }

    fun startOrResumeWorkout() {
        val today = todayDateString()
        val currentDay = _uiState.value.selectedDay

        val existing = _uiState.value.history.firstOrNull {
            it.date == today && it.day == currentDay
        }

        if (existing != null) {
            _uiState.update { it.copy(currentSession = existing) }
            return
        }

        val dayExercises = _uiState.value.plans[currentDay] ?: emptyList()
        val newSession = WorkoutSession(
            id = UUID.randomUUID().toString(),
            date = today,
            day = currentDay,
            exercises = dayExercises.map { exercise ->
                ExerciseWorkout(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    sets = (1..exercise.targetSets).map { num ->
                        WorkoutSet(
                            setNumber = num,
                            targetReps = exercise.targetReps,
                            targetWeight = exercise.targetWeight
                        )
                    }
                )
            }
        )

        _uiState.update { it.copy(currentSession = newSession) }

        viewModelScope.launch {
            repository.saveWorkoutSession(newSession)
        }
    }

    fun updateSessionInProgress(updated: WorkoutSession) {
        _uiState.update { current ->
            val updatedHistory = current.history.filterNot { it.id == updated.id } + updated
            current.copy(
                currentSession = updated,
                history = updatedHistory
            )
        }
    }

    fun saveWorkoutSession(session: WorkoutSession) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val result = repository.saveWorkoutSession(session)
            if (result.isSuccess) {
                val saved = result.getOrNull() ?: session
                _uiState.update {
                    it.copy(
                        currentSession = saved,
                        isSyncing = false,
                        successMessage = "Workout saved successfully!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        errorMessage = "Workout saved locally. Backend sync failed."
                    )
                }
            }
        }
    }

    fun deleteWorkoutSession(id: String) {
        viewModelScope.launch {
            repository.deleteWorkoutSession(id)
            _uiState.update { current ->
                val remaining = current.history.filterNot { it.id == id }
                val newCurrent = if (current.currentSession?.id == id) null else current.currentSession
                current.copy(
                    history = remaining,
                    currentSession = newCurrent
                )
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    private fun currentDayAbbreviation(): String {
        val raw = SimpleDateFormat("EEE", Locale.US).format(Date()).take(3).replaceFirstChar { it.uppercase() }
        val validDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return if (raw in validDays) raw else "Mon"
    }

    private fun todayDateString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as MainApplication
                PlannerViewModel(application.container.plannerRepository)
            }
        }
    }
}
