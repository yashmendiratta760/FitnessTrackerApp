package com.yash.Fitness.Tracker.Repository;


import com.yash.Fitness.Tracker.entity.WorkoutSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface WorkoutSessionRepository extends MongoRepository<WorkoutSession, String> {
    List<WorkoutSession> findByUserIdOrderByDateDesc(String userId);
    Optional<WorkoutSession> findByIdAndUserId(String id, String userId);
    Optional<WorkoutSession> findFirstByUserIdAndDateAndDay(String userId, String date, String day);
}
