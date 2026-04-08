package com.yash.Fitness.Tracker.service;

import com.yash.Fitness.Tracker.DTO.StepsDTO;
import com.yash.Fitness.Tracker.Repository.StepsRepository;
import com.yash.Fitness.Tracker.entity.StepsEntity;
import com.yash.Fitness.Tracker.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StepsService
{

    @Autowired
    private StepsRepository stepsRepository;

    @Autowired
    private UserService userService;

    public void saveEntry(StepsDTO stepsDTO,String userName)
    {

        StepsEntity stepsEntity = new StepsEntity();
        stepsEntity.setSteps(stepsDTO.getSteps());
        stepsEntity.setDate(stepsDTO.getDate());
        StepsEntity saved =  stepsRepository.save(stepsEntity);
        UserEntity user = userService.findByUserName(userName);
        user.getStepsEntity().add(saved);
        userService.updateUser(user);

    }

    public List<StepsEntity> getAllEntries(String userName)
    {
        UserEntity user = userService.findByUserName(userName);
        return user.getStepsEntity();
    }


}
