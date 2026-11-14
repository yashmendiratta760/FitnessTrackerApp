package com.yash.Fitness.Tracker.controller;

import com.yash.Fitness.Tracker.DTO.PlanRequest;
import com.yash.Fitness.Tracker.entity.WeeklyPlan;
import com.yash.Fitness.Tracker.entity.WorkoutSession;
import com.yash.Fitness.Tracker.service.PlannerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/planner", "/planner"})
public class PlannerController {
    private final PlannerService service;

    public PlannerController(PlannerService service) {
        this.service = service;
    }

    @GetMapping("/{userId}/plans")
    public List<WeeklyPlan> getWeek(@PathVariable String userId) {
        return service.getWeek(userId);
    }

    @GetMapping("/{userId}/plans/{day}")
    public WeeklyPlan getDay(@PathVariable String userId, @PathVariable String day) {
        return service.getDay(userId, day);
    }

    @PutMapping("/{userId}/plans/{day}")
    public WeeklyPlan saveDay(@PathVariable String userId, @PathVariable String day,
                              @RequestBody PlanRequest request) {
        return service.saveDay(userId, day, request);
    }

    @DeleteMapping("/{userId}/plans/{day}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDay(@PathVariable String userId, @PathVariable String day) {
        service.deleteDay(userId, day);
    }

    @GetMapping("/{userId}/workouts")
    public List<WorkoutSession> history(@PathVariable String userId) {
        return service.getHistory(userId);
    }

    @GetMapping("/{userId}/workouts/{id}")
    public WorkoutSession getWorkout(@PathVariable String userId, @PathVariable String id) {
        return service.getSession(userId, id);
    }

    @GetMapping("/{userId}/workouts/by-date")
    public WorkoutSession getWorkoutForDate(@PathVariable String userId,
                                            @RequestParam String date,
                                            @RequestParam String day) {
        return service.getSessionForDate(userId, date, day);
    }

    @PostMapping("/{userId}/workouts")
    public WorkoutSession createOrSaveWorkout(@PathVariable String userId,
                                               @RequestBody WorkoutSession session) {
        return service.saveSession(userId, session);
    }

    @PutMapping("/{userId}/workouts/{id}")
    public WorkoutSession updateWorkout(@PathVariable String userId, @PathVariable String id,
                                        @RequestBody WorkoutSession session) {
        session.setId(id);
        return service.saveSession(userId, session);
    }

    @DeleteMapping("/{userId}/workouts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkout(@PathVariable String userId, @PathVariable String id) {
        service.deleteSession(userId, id);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "fitness-planner-backend");
    }
}
