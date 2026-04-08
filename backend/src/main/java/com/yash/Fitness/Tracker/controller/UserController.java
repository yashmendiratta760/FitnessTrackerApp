package com.yash.Fitness.Tracker.controller;


import com.yash.Fitness.Tracker.DTO.*;
import com.yash.Fitness.Tracker.cache.OtpCache;
import com.yash.Fitness.Tracker.entity.UserEntity;
import com.yash.Fitness.Tracker.service.UserService;
import com.yash.Fitness.Tracker.utils.JWTUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JWTUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailController emailController;

    @Autowired
    private OtpCache otpCache;




    @GetMapping("/check")
    public ResponseEntity<?> check()
    {
        return new ResponseEntity<>("OK",HttpStatus.OK);
    }
    @PostMapping("/generate-otp")
    public ResponseEntity<String> generateOtp(@RequestBody UserDTO user) {

        try {
            if (userService.existsByUserName(user.getUserName())) {
                log.info("Username already exists: {}", user.getUserName());
                return new ResponseEntity<>("Username already exists", HttpStatus.CONFLICT);
            }
            TempUserData tempUserData = new TempUserData();
            String otp = EmailController.generateOtp();
            emailController.sendOtp(user.getEmail(),otp);
            log.info("otp sent");
            tempUserData.setUserDTO(user);
            tempUserData.setOtp(otp);

            otpCache.enterData(tempUserData.getUserDTO().getEmail(),tempUserData);

            return new ResponseEntity<>(otp, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.toString(),HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PostMapping("/signup")
    public ResponseEntity<?> createUser(@RequestBody otpValidate otpValidate)
    {
        try {
            TempUserData userData = otpCache.getData(otpValidate.getEmail());
            if(userData!=null && otpValidate.getOtp().equals(userData.getOtp()))
            {
                userData.getUserDTO().setPassword(passwordEncoder.encode(userData.getUserDTO().getPassword()));
                userService.createUser(userData.getUserDTO());
                otpCache.removeData(otpValidate.getEmail());
                log.info("otp validated");
                String token = jwtUtils.generateToken(userData.getUserDTO().getUserName());
                return ResponseEntity.ok(new JwtResponse(token));
            }
            else {
                log.info("otp not validated");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            log.error("error in signup",e.toString());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginDTO loginDTO) {
        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginDTO.getUserName(), loginDTO.getPassword());

            Authentication authentication = authenticationManager.authenticate(authenticationToken);


            // Generate JWT token if authentication is successful
            String token = jwtUtils.generateToken(authentication.getName());
            return ResponseEntity.ok(new JwtResponse(token));

        } catch (BadCredentialsException e) {
            // Return 401 Unauthorized if credentials are incorrect
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }

    @PostMapping("/getEmail")
    public ResponseEntity<String> getEmail(@RequestBody LoginDTO loginDTO)
    {
        UserEntity user = userService.findByUserName(loginDTO.getUserName());

        String email = user.getEmail();

        return ResponseEntity.ok(email);
    }

}