package com.yash.Fitness.Tracker.Repository;

import com.yash.Fitness.Tracker.entity.StepsEntity;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StepsRepository extends MongoRepository<StepsEntity, ObjectId>
{

}
