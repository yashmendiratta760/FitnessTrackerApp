package com.yash.fitnesstracker.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "planner_days")
data class PlannerDayEntity(
    @PrimaryKey val day: String,
    val exercisesJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val date: String,
    val day: String,
    val exercisesJson: String,
    val isSynced: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface PlannerDao {
    @Query("SELECT * FROM planner_days")
    fun getAllDayPlans(): Flow<List<PlannerDayEntity>>

    @Query("SELECT * FROM planner_days")
    suspend fun getAllDayPlansList(): List<PlannerDayEntity>

    @Query("SELECT * FROM planner_days WHERE day = :day LIMIT 1")
    suspend fun getDayPlan(day: String): PlannerDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayPlan(dayPlan: PlannerDayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayPlans(dayPlans: List<PlannerDayEntity>)

    @Query("DELETE FROM planner_days WHERE day = :day")
    suspend fun deleteDayPlan(day: String)

    @Query("DELETE FROM planner_days")
    suspend fun clearDayPlans()

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC, updatedAt DESC")
    fun getAllWorkoutSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC, updatedAt DESC")
    suspend fun getAllWorkoutSessionsList(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE id = :id LIMIT 1")
    suspend fun getWorkoutSessionById(id: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE date = :date AND day = :day LIMIT 1")
    suspend fun getWorkoutSessionByDate(date: String, day: String): WorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(session: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSessions(sessions: List<WorkoutSessionEntity>)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteWorkoutSession(id: String)

    @Query("DELETE FROM workout_sessions")
    suspend fun clearWorkoutSessions()
}

@Database(
    entities = [PlannerDayEntity::class, WorkoutSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PlannerDatabase : RoomDatabase() {
    abstract fun plannerDao(): PlannerDao

    companion object {
        @Volatile
        private var instance: PlannerDatabase? = null

        fun getDatabase(context: Context): PlannerDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    PlannerDatabase::class.java,
                    "planner_db"
                ).build().also { instance = it }
            }
        }
    }
}
