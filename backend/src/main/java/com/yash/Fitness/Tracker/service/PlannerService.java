package com.yash.Fitness.Tracker.service;


import com.yash.Fitness.Tracker.DTO.PlanRequest;
import com.yash.Fitness.Tracker.Repository.WeeklyPlanRepository;
import com.yash.Fitness.Tracker.Repository.WorkoutSessionRepository;
import com.yash.Fitness.Tracker.entity.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class PlannerService {
    private static final List<String> DAYS = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
    private final WeeklyPlanRepository plans;
    private final WorkoutSessionRepository sessions;

    public PlannerService(WeeklyPlanRepository plans, WorkoutSessionRepository sessions) {
        this.plans = plans;
        this.sessions = sessions;
    }

    public List<WeeklyPlan> getWeek(String userId) {
        requireUser(userId);
        Map<String, WeeklyPlan> byDay = new HashMap<>();
        plans.findByUserIdOrderByDayAsc(userId).forEach(p -> byDay.put(p.getDay(), p));
        List<WeeklyPlan> result = new ArrayList<>();
        for (String day : DAYS) {
            WeeklyPlan plan = byDay.get(day);
            if (plan == null) {
                plan = new WeeklyPlan();
                plan.setUserId(userId);
                plan.setDay(day);
                plan.setExercises(new ArrayList<>());
            }
            result.add(plan);
        }
        return result;
    }

    public WeeklyPlan getDay(String userId, String day) {
        requireUser(userId);
        validateDay(day);
        return plans.findByUserIdAndDay(userId, day).orElseGet(() -> {
            WeeklyPlan plan = new WeeklyPlan();
            plan.setUserId(userId);
            plan.setDay(day);
            return plan;
        });
    }

    public WeeklyPlan saveDay(String userId, String day, PlanRequest request) {
        requireUser(userId);
        validateDay(day);
        WeeklyPlan plan = plans.findByUserIdAndDay(userId, day).orElseGet(WeeklyPlan::new);
        List<PlannedExercise> exercises = request.getExercises();
        for (PlannedExercise exercise : exercises) {
            if (exercise.getId() == null || exercise.getId().isBlank()) {
                exercise.setId(UUID.randomUUID().toString());
            }
            if (exercise.getName() == null || exercise.getName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exercise name is required");
            }
            if (exercise.getTargetSets() < 1 || exercise.getTargetReps() < 1 || exercise.getTargetWeight() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exercise targets are invalid");
            }
        }
        plan.setUserId(userId);
        plan.setDay(day);
        plan.setExercises(exercises);
        plan.setUpdatedAt(Instant.now());
        return plans.save(plan);
    }

    public void deleteDay(String userId, String day) {
        requireUser(userId);
        validateDay(day);
        plans.deleteByUserIdAndDay(userId, day);
    }

    public List<WorkoutSession> getHistory(String userId) {
        requireUser(userId);
        return sessions.findByUserIdOrderByDateDesc(userId);
    }

    public WorkoutSession getSession(String userId, String id) {
        requireUser(userId);
        return sessions.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout session not found"));
    }

    public WorkoutSession getSessionForDate(String userId, String date, String day) {
        requireUser(userId);
        validateDay(day);
        return sessions.findFirstByUserIdAndDateAndDay(userId, date, day)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout session not found"));
    }

    public WorkoutSession saveSession(String userId, WorkoutSession incoming) {
        requireUser(userId);
        if (incoming.getDate() == null || incoming.getDate().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date is required in yyyy-MM-dd format");
        }
        validateDay(incoming.getDay());

        if (incoming.getExercises() == null) incoming.setExercises(new ArrayList<>());
        for (WorkoutExercise exercise : incoming.getExercises()) {
            if (exercise.getExerciseId() == null || exercise.getExerciseId().isBlank()) {
                exercise.setExerciseId(UUID.randomUUID().toString());
            }
            if (exercise.getSets() == null) exercise.setSets(new ArrayList<>());
            for (WorkoutSet set : exercise.getSets()) {
                if (set.getSetNumber() < 1 || set.getTargetReps() < 1 ||
                        set.getActualReps() < 0 || set.getTargetWeight() < 0 ||
                        set.getActualWeight() < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Set values are invalid");
                }
            }
        }

        WorkoutSession toSave;
        if (incoming.getId() != null && !incoming.getId().isBlank()) {
            toSave = sessions.findByIdAndUserId(incoming.getId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout session not found"));
            incoming.setCreatedAt(toSave.getCreatedAt());
        } else {
            // One session per user/day/date. If the client retries a POST, update that session.
            toSave = sessions.findFirstByUserIdAndDateAndDay(userId, incoming.getDate(), incoming.getDay()).orElse(null);
            if (toSave != null) {
                incoming.setId(toSave.getId());
                incoming.setCreatedAt(toSave.getCreatedAt());
            } else {
                incoming.setId(null);
                incoming.setCreatedAt(Instant.now());
            }
        }
        incoming.setUserId(userId);
        incoming.setUpdatedAt(Instant.now());
        return sessions.save(incoming);
    }

    public void deleteSession(String userId, String id) {
        requireUser(userId);
        WorkoutSession session = getSession(userId, id);
        sessions.delete(session);
    }

    private void validateDay(String day) {
        if (day == null || !DAYS.contains(day)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "day must be one of Mon, Tue, Wed, Thu, Fri, Sat, Sun");
        }
    }

    private void requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
    }
}
