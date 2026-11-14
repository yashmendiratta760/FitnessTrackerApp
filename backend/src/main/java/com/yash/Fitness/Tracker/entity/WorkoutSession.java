package com.yash.Fitness.Tracker.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("workout_sessions")
public class WorkoutSession {
    @Id private String id;
    private String userId;
    private String date; // yyyy-MM-dd
    private String day;  // Mon, Tue, Wed, Thu, Fri, Sat, Sun
    private List<WorkoutExercise> exercises = new ArrayList<>();
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public WorkoutSession() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }
    public List<WorkoutExercise> getExercises() { return exercises; }
    public void setExercises(List<WorkoutExercise> exercises) { this.exercises = exercises == null ? new ArrayList<>() : exercises; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
