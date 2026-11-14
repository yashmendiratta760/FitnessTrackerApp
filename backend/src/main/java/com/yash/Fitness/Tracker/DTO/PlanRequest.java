package com.yash.Fitness.Tracker.DTO;

import com.yash.Fitness.Tracker.entity.PlannedExercise;

import java.util.ArrayList;
import java.util.List;

public class PlanRequest {
    private List<PlannedExercise> exercises = new ArrayList<>();
    public List<PlannedExercise> getExercises() { return exercises; }
    public void setExercises(List<PlannedExercise> exercises) { this.exercises = exercises == null ? new ArrayList<>() : exercises; }
}
