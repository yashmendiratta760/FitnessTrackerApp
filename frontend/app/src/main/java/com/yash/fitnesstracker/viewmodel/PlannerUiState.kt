package com.yash.fitnesstracker.viewmodel

import com.yash.fitnesstracker.api.PlannedExercise
import com.yash.fitnesstracker.api.WorkoutSession

data class PlannerUiState(
    val selectedDay: String = "Mon",
    val plans: Map<String, List<PlannedExercise>> = emptyMap(),
    val history: List<WorkoutSession> = emptyList(),
    val currentSession: WorkoutSession? = null,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isBackendHealthy: Boolean? = null
)
