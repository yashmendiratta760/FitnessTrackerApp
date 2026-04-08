package com.yash.Fitness.Tracker.controller;


import com.yash.Fitness.Tracker.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class EmailController
{

    @Autowired
    private EmailService emailService;

    private static final SecureRandom random = new SecureRandom();

    public void sendOtp(String to,String otp)
    {
        emailService.sendMail(to,"Otp for Authentication","Your Otp is : " + otp);
    }

    public static String generateOtp() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}
