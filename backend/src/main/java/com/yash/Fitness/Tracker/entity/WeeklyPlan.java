package com.yash.Fitness.Tracker.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("weekly_plans")
public class WeeklyPlan {
    @Id private String id;
    private String userId;
    private String day;
    private List<PlannedExercise> exercises = new ArrayList<>();
    private Instant updatedAt = Instant.now();

    public WeeklyPlan() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }
    public List<PlannedExercise> getExercises() { return exercises; }
    public void setExercises(List<PlannedExercise> exercises) { this.exercises = exercises == null ? new ArrayList<>() : exercises; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
