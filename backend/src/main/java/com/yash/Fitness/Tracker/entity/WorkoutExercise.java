package com.yash.Fitness.Tracker.entity;

import java.util.ArrayList;
import java.util.List;

public class WorkoutExercise {
    private String exerciseId;
    private String exerciseName;
    private boolean isCompleted;
    private List<WorkoutSet> sets = new ArrayList<>();

    public WorkoutExercise() {}
    public String getExerciseId() { return exerciseId; }
    public void setExerciseId(String exerciseId) { this.exerciseId = exerciseId; }
    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }
    public boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(boolean completed) { this.isCompleted = completed; }
    public List<WorkoutSet> getSets() { return sets; }
    public void setSets(List<WorkoutSet> sets) { this.sets = sets == null ? new ArrayList<>() : sets; }
}
