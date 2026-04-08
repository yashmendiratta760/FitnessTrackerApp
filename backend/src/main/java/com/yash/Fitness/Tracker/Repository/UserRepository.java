package com.yash.Fitness.Tracker.Repository;

import com.yash.Fitness.Tracker.entity.UserEntity;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<UserEntity, ObjectId>
{
    UserEntity findByUserName(String username);
    boolean existsByUserName(String userName);

    UserEntity findByEmail(String email);

    boolean existsByEmail(String email);

}
