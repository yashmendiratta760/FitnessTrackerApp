package com.yash.Fitness.Tracker.Repository;

import com.yash.Fitness.Tracker.entity.WeeklyPlan;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface WeeklyPlanRepository extends MongoRepository<WeeklyPlan, String> {
    List<WeeklyPlan> findByUserIdOrderByDayAsc(String userId);
    Optional<WeeklyPlan> findByUserIdAndDay(String userId, String day);
    void deleteByUserIdAndDay(String userId, String day);
}
