package com.yash.Fitness.Tracker.entity;

public class PlannedExercise {
    private String id;
    private String name;
    private int targetSets = 3;
    private int targetReps = 10;
    private double targetWeight;

    public PlannedExercise() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getTargetSets() { return targetSets; }
    public void setTargetSets(int targetSets) { this.targetSets = targetSets; }
    public int getTargetReps() { return targetReps; }
    public void setTargetReps(int targetReps) { this.targetReps = targetReps; }
    public double getTargetWeight() { return targetWeight; }
    public void setTargetWeight(double targetWeight) { this.targetWeight = targetWeight; }
}
