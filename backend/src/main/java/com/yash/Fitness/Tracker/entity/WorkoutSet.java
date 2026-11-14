package com.yash.Fitness.Tracker.entity;

public class WorkoutSet {
    private int setNumber;
    private int targetReps;
    private double targetWeight;
    private int actualReps;
    private double actualWeight;
    private boolean isCompleted;

    public WorkoutSet() {}
    public int getSetNumber() { return setNumber; }
    public void setSetNumber(int setNumber) { this.setNumber = setNumber; }
    public int getTargetReps() { return targetReps; }
    public void setTargetReps(int targetReps) { this.targetReps = targetReps; }
    public double getTargetWeight() { return targetWeight; }
    public void setTargetWeight(double targetWeight) { this.targetWeight = targetWeight; }
    public int getActualReps() { return actualReps; }
    public void setActualReps(int actualReps) { this.actualReps = actualReps; }
    public double getActualWeight() { return actualWeight; }
    public void setActualWeight(double actualWeight) { this.actualWeight = actualWeight; }
    public boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(boolean completed) { this.isCompleted = completed; }
}
