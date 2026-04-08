package com.yash.Fitness.Tracker.service;


import com.yash.Fitness.Tracker.DTO.UserDTO;
import com.yash.Fitness.Tracker.Repository.UserRepository;
import com.yash.Fitness.Tracker.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class UserService
{

    @Autowired
    private UserRepository userRepository;

    public void createUser(UserDTO user)
    {
        UserEntity userE = new UserEntity();
        userE.setUserName(user.getUserName());
        userE.setPassword(user.getPassword());
        userE.setEmail(user.getEmail());
        userRepository.save(userE);
    }


    public void updateUser(UserEntity user) {
        userRepository.save(user);
    }

    public boolean existsByUserName(String userName) {
        return userRepository.existsByUserName(userName);
    }

    public UserEntity findByUserName(String userName)
    {
        return userRepository.findByUserName(userName);
    }

    public boolean existsByEmail(String Email) {
        return userRepository.existsByEmail(Email);
    }

    public UserEntity findByEmail(String Email)
    {
        return userRepository.findByEmail(Email);
    }
}

