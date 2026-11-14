package com.yash.fitnesstracker.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yash.fitnesstracker.api.ExerciseWorkout
import com.yash.fitnesstracker.api.PlanRequest
import com.yash.fitnesstracker.api.PlannedExercise
import com.yash.fitnesstracker.api.PlannerApi
import com.yash.fitnesstracker.api.WorkoutSession
import com.yash.fitnesstracker.database.PlannerDao
import com.yash.fitnesstracker.database.PlannerDayEntity
import com.yash.fitnesstracker.database.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface PlannerRepository {
    val weeklyPlansFlow: Flow<Map<String, List<PlannedExercise>>>
    val workoutHistoryFlow: Flow<List<WorkoutSession>>

    suspend fun syncWeeklyPlans(userId: String = "demo-user"): Result<Map<String, List<PlannedExercise>>>
    suspend fun getDayPlan(day: String, userId: String = "demo-user"): List<PlannedExercise>
    suspend fun saveDayPlan(day: String, exercises: List<PlannedExercise>, userId: String = "demo-user"): Result<List<PlannedExercise>>
    suspend fun deleteDayPlan(day: String, userId: String = "demo-user"): Result<Unit>

    suspend fun syncWorkoutHistory(userId: String = "demo-user"): Result<List<WorkoutSession>>
    suspend fun saveWorkoutSession(session: WorkoutSession, userId: String = "demo-user"): Result<WorkoutSession>
    suspend fun deleteWorkoutSession(id: String, userId: String = "demo-user"): Result<Unit>
    suspend fun getWorkoutSessionForDate(date: String, day: String, userId: String = "demo-user"): WorkoutSession?

    suspend fun checkBackendHealth(): Boolean
}

class PlannerRepositoryImpl(
    private val api: PlannerApi,
    private val dao: PlannerDao,
    private val gson: Gson = Gson()
) : PlannerRepository {

    private val plannedExerciseListType = object : TypeToken<List<PlannedExercise>>() {}.type
    private val workoutExerciseListType = object : TypeToken<List<ExerciseWorkout>>() {}.type

    override val weeklyPlansFlow: Flow<Map<String, List<PlannedExercise>>> =
        dao.getAllDayPlans().map { entities ->
            entities.associate { entity ->
                val exercises: List<PlannedExercise> = try {
                    gson.fromJson(entity.exercisesJson, plannedExerciseListType) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
                entity.day to exercises
            }
        }

    override val workoutHistoryFlow: Flow<List<WorkoutSession>> =
        dao.getAllWorkoutSessions().map { entities ->
            entities.map { entity ->
                val exercises: List<ExerciseWorkout> = try {
                    gson.fromJson(entity.exercisesJson, workoutExerciseListType) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
                WorkoutSession(
                    id = entity.id,
                    date = entity.date,
                    day = entity.day,
                    exercises = exercises
                )
            }
        }

    override suspend fun syncWeeklyPlans(userId: String): Result<Map<String, List<PlannedExercise>>> {
        return try {
            val response = api.getWeeklyPlans(userId)
            if (response.isSuccessful && response.body() != null) {
                val plansList = response.body()!!
                val entities = plansList.map { planDto ->
                    PlannerDayEntity(
                        day = planDto.day,
                        exercisesJson = gson.toJson(planDto.exercises)
                    )
                }
                dao.insertDayPlans(entities)

                val result = plansList.associate { it.day to it.exercises }
                Result.success(result)
            } else {
                val errorMsg = "Failed to fetch plans: HTTP ${response.code()} ${response.message()}"
                Log.e("PlannerRepo", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("PlannerRepo", "Error syncing weekly plans", e)
            Result.failure(e)
        }
    }

    override suspend fun getDayPlan(day: String, userId: String): List<PlannedExercise> {
        val cached = dao.getDayPlan(day)
        if (cached != null) {
            return try {
                gson.fromJson(cached.exercisesJson, plannedExerciseListType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
        return try {
            val response = api.getDayPlan(userId, day)
            if (response.isSuccessful && response.body() != null) {
                val exercises = response.body()!!.exercises
                dao.insertDayPlan(
                    PlannerDayEntity(day = day, exercisesJson = gson.toJson(exercises))
                )
                exercises
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun saveDayPlan(
        day: String,
        exercises: List<PlannedExercise>,
        userId: String
    ): Result<List<PlannedExercise>> {
        // 1. Immediately cache in Room DB for offline resilience
        dao.insertDayPlan(
            PlannerDayEntity(day = day, exercisesJson = gson.toJson(exercises))
        )

        // 2. Persist to MongoDB backend
        return try {
            val response = api.saveDayPlan(userId, day, PlanRequest(exercises))
            if (response.isSuccessful && response.body() != null) {
                val savedExercises = response.body()!!.exercises
                dao.insertDayPlan(
                    PlannerDayEntity(day = day, exercisesJson = gson.toJson(savedExercises))
                )
                Result.success(savedExercises)
            } else {
                val errorMsg = "Backend save failed: HTTP ${response.code()} ${response.message()}"
                Log.w("PlannerRepo", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.w("PlannerRepo", "Network error saving day plan; cached locally in Room", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteDayPlan(day: String, userId: String): Result<Unit> {
        dao.deleteDayPlan(day)
        return try {
            val response = api.deleteDayPlan(userId, day)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete day plan: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.w("PlannerRepo", "Network error deleting day plan; deleted locally in Room", e)
            Result.failure(e)
        }
    }

    override suspend fun syncWorkoutHistory(userId: String): Result<List<WorkoutSession>> {
        return try {
            val response = api.getAllWorkouts(userId)
            if (response.isSuccessful && response.body() != null) {
                val workouts = response.body()!!
                val entities = workouts.map { session ->
                    WorkoutSessionEntity(
                        id = session.id,
                        date = session.date,
                        day = session.day,
                        exercisesJson = gson.toJson(session.exercises),
                        isSynced = true
                    )
                }
                dao.insertWorkoutSessions(entities)
                Result.success(workouts)
            } else {
                val errorMsg = "Failed to fetch workouts: HTTP ${response.code()}"
                Log.e("PlannerRepo", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("PlannerRepo", "Error syncing workout history", e)
            Result.failure(e)
        }
    }

    override suspend fun saveWorkoutSession(
        session: WorkoutSession,
        userId: String
    ): Result<WorkoutSession> {
        val localId = session.id
        // 1. Save to Room immediately
        dao.insertWorkoutSession(
            WorkoutSessionEntity(
                id = localId,
                date = session.date,
                day = session.day,
                exercisesJson = gson.toJson(session.exercises),
                isSynced = false
            )
        )

        // 2. Sync to MongoDB backend
        return try {
            val isServerMongoId = isMongoHexId(localId)
            val sessionToSend = if (isServerMongoId) session else session.copy(id = "")

            val response = if (isServerMongoId) {
                api.updateWorkout(userId, localId, sessionToSend)
            } else {
                api.createOrSaveWorkout(userId, sessionToSend)
            }

            if (response.isSuccessful && response.body() != null) {
                val serverSession = response.body()!!
                // If local id was temporary UUID, remove temporary entry and insert server entry
                if (serverSession.id != localId) {
                    dao.deleteWorkoutSession(localId)
                }
                dao.insertWorkoutSession(
                    WorkoutSessionEntity(
                        id = serverSession.id,
                        date = serverSession.date,
                        day = serverSession.day,
                        exercisesJson = gson.toJson(serverSession.exercises),
                        isSynced = true
                    )
                )
                Result.success(serverSession)
            } else {
                val msg = "Backend save workout failed: HTTP ${response.code()} ${response.message()}"
                Log.w("PlannerRepo", msg)
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.w("PlannerRepo", "Network error saving workout; cached locally in Room", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteWorkoutSession(id: String, userId: String): Result<Unit> {
        dao.deleteWorkoutSession(id)
        return try {
            val response = api.deleteWorkout(userId, id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete workout: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.w("PlannerRepo", "Network error deleting workout; deleted locally in Room", e)
            Result.failure(e)
        }
    }

    override suspend fun getWorkoutSessionForDate(
        date: String,
        day: String,
        userId: String
    ): WorkoutSession? {
        val cached = dao.getWorkoutSessionByDate(date, day)
        if (cached != null) {
            val exercises: List<ExerciseWorkout> = try {
                gson.fromJson(cached.exercisesJson, workoutExerciseListType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            return WorkoutSession(
                id = cached.id,
                date = cached.date,
                day = cached.day,
                exercises = exercises
            )
        }

        return try {
            val response = api.getWorkoutByDate(userId, date, day)
            if (response.isSuccessful && response.body() != null) {
                val session = response.body()!!
                dao.insertWorkoutSession(
                    WorkoutSessionEntity(
                        id = session.id,
                        date = session.date,
                        day = session.day,
                        exercisesJson = gson.toJson(session.exercises),
                        isSynced = true
                    )
                )
                session
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun checkBackendHealth(): Boolean {
        return try {
            val response = api.healthCheck()
            response.isSuccessful && response.body()?.get("status") == "UP"
        } catch (e: Exception) {
            Log.w("PlannerRepo", "Backend health check failed", e)
            false
        }
    }

    private fun isMongoHexId(id: String): Boolean {
        return id.length == 24 && id.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }
}
