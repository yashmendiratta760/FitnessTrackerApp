package com.yash.Fitness.Tracker.controller;


import com.yash.Fitness.Tracker.DTO.StepsDTO;
import com.yash.Fitness.Tracker.entity.StepsEntity;
import com.yash.Fitness.Tracker.entity.UserEntity;
import com.yash.Fitness.Tracker.service.StepsService;
import com.yash.Fitness.Tracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/steps")
public class StepsController
{

    @Autowired
    private StepsService stepsService;

    @Autowired
    private UserService userService;

    @PostMapping("create-entry")
    public ResponseEntity<?> createEntry(@RequestBody StepsDTO stepsDTO)
    {
        Authentication authentication   = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        UserEntity user =  userService.findByUserName(userName);
        if(user!=null)
        {
            stepsService.saveEntry(stepsDTO,user.getUserName());
            return new ResponseEntity<>(HttpStatus.OK);
        }
        else
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

    }

    @GetMapping("getAll")
    public ResponseEntity<?> getAllEntries()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        UserEntity user = userService.findByUserName(userName);
        if(user!=null)
        {
            List<StepsEntity> stepData =  stepsService.getAllEntries(userName);
            List<StepsDTO> steps = stepData.stream()
                    .map(data -> new StepsDTO(data.getSteps(),data.getDate()))
                    .toList();

            return new ResponseEntity<>(steps,HttpStatus.OK);
        }
        else
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
