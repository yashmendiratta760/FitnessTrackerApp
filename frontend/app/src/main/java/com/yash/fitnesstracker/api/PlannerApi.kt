package com.yash.fitnesstracker.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PlannerApi {
    @GET("api/planner/{userId}/plans")
    suspend fun getWeeklyPlans(
        @Path("userId") userId: String = "demo-user"
    ): Response<List<WeeklyPlanDto>>

    @GET("api/planner/{userId}/plans/{day}")
    suspend fun getDayPlan(
        @Path("userId") userId: String = "demo-user",
        @Path("day") day: String
    ): Response<WeeklyPlanDto>

    @PUT("api/planner/{userId}/plans/{day}")
    suspend fun saveDayPlan(
        @Path("userId") userId: String = "demo-user",
        @Path("day") day: String,
        @Body request: PlanRequest
    ): Response<WeeklyPlanDto>

    @DELETE("api/planner/{userId}/plans/{day}")
    suspend fun deleteDayPlan(
        @Path("userId") userId: String = "demo-user",
        @Path("day") day: String
    ): Response<Unit>

    @GET("api/planner/{userId}/workouts")
    suspend fun getAllWorkouts(
        @Path("userId") userId: String = "demo-user"
    ): Response<List<WorkoutSession>>

    @GET("api/planner/{userId}/workouts/{id}")
    suspend fun getWorkoutById(
        @Path("userId") userId: String = "demo-user",
        @Path("id") id: String
    ): Response<WorkoutSession>

    @GET("api/planner/{userId}/workouts/by-date")
    suspend fun getWorkoutByDate(
        @Path("userId") userId: String = "demo-user",
        @Query("date") date: String,
        @Query("day") day: String
    ): Response<WorkoutSession>

    @POST("api/planner/{userId}/workouts")
    suspend fun createOrSaveWorkout(
        @Path("userId") userId: String = "demo-user",
        @Body session: WorkoutSession
    ): Response<WorkoutSession>

    @PUT("api/planner/{userId}/workouts/{id}")
    suspend fun updateWorkout(
        @Path("userId") userId: String = "demo-user",
        @Path("id") id: String,
        @Body session: WorkoutSession
    ): Response<WorkoutSession>

    @DELETE("api/planner/{userId}/workouts/{id}")
    suspend fun deleteWorkout(
        @Path("userId") userId: String = "demo-user",
        @Path("id") id: String
    ): Response<Unit>

    @GET("api/planner/health")
    suspend fun healthCheck(): Response<Map<String, String>>
}
