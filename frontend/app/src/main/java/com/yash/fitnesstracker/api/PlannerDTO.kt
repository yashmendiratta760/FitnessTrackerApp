package com.yash.fitnesstracker.api

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class PlannedExercise(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("name") val name: String,
    @SerializedName("targetSets") val targetSets: Int = 3,
    @SerializedName("targetReps") val targetReps: Int = 10,
    @SerializedName("targetWeight") val targetWeight: Float = 0f
)

data class PlanRequest(
    @SerializedName("exercises") val exercises: List<PlannedExercise> = emptyList()
)

data class WeeklyPlanDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("day") val day: String,
    @SerializedName("exercises") val exercises: List<PlannedExercise> = emptyList(),
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class WorkoutSet(
    @SerializedName("setNumber") val setNumber: Int = 1,
    @SerializedName("targetReps") val targetReps: Int = 10,
    @SerializedName("targetWeight") val targetWeight: Float = 0f,
    @SerializedName("actualReps") val actualReps: Int = 0,
    @SerializedName("actualWeight") val actualWeight: Float = 0f,
    @SerializedName(value = "isCompleted", alternate = ["completed"]) val isCompleted: Boolean = false
)

data class ExerciseWorkout(
    @SerializedName("exerciseId") val exerciseId: String = UUID.randomUUID().toString(),
    @SerializedName("exerciseName") val exerciseName: String = "",
    @SerializedName(value = "isCompleted", alternate = ["completed"]) val isCompleted: Boolean = false,
    @SerializedName("sets") val sets: List<WorkoutSet> = emptyList()
)

data class WorkoutSession(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("userId") val userId: String? = "demo-user",
    @SerializedName("date") val date: String,
    @SerializedName("day") val day: String,
    @SerializedName("exercises") val exercises: List<ExerciseWorkout> = emptyList(),
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class HealthResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("service") val service: String? = null
)
