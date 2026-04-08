package com.yash.Fitness.Tracker.controller;

import com.cloudinary.Cloudinary;
import com.yash.Fitness.Tracker.entity.UserEntity;
import com.yash.Fitness.Tracker.service.CloudinaryService;
import com.yash.Fitness.Tracker.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/cloudinary")
public class CloudinaryController
{

    @Autowired
    private UserService userService;
    private final CloudinaryService cloudinaryService;


    public CloudinaryController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping("upload")
    public ResponseEntity<?> uploadImage(@RequestParam("userName") String userName,
                                              @RequestParam("file")MultipartFile file)
    {
        try {
            log.error("clicked");
            String imageUrl = cloudinaryService.uploadImage(file);
            UserEntity user = userService.findByUserName(userName);
            if (user == null) {
                return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
            }
            user.setImageUrl(imageUrl);
            userService.updateUser(user);
            log.error(imageUrl);
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("getImage")
    public ResponseEntity<?> getImageUrl()
    {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userName = authentication.getName();

            UserEntity user = userService.findByUserName(userName);
            if(user==null) return new ResponseEntity<>("User not=t Found",HttpStatus.OK);
            return ResponseEntity.ok(user.getImageUrl());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
