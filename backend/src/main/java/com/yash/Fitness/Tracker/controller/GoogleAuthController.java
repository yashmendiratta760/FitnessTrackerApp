package com.yash.Fitness.Tracker.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.yash.Fitness.Tracker.DTO.JwtResponse;
import com.yash.Fitness.Tracker.DTO.JwtResponseG;
import com.yash.Fitness.Tracker.DTO.UserDTO;
import com.yash.Fitness.Tracker.entity.UserEntity;
import com.yash.Fitness.Tracker.service.UserService;
import com.yash.Fitness.Tracker.utils.JWTUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/auth")
public class GoogleAuthController {


    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JWTUtils jwtUtils;


    @Value("${google.client_id}")
    private String ClientId;



    @PostMapping("/google")
    public ResponseEntity<?> verifyGoogleToken(@RequestBody Map<String, String> body) {
        String idTokenString = body.get("idToken");




        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier
                .Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(ClientId))
                .build();

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid ID token.");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            UserEntity user = userService.findByEmail(email);
            if(user==null)
            {
                user = new UserEntity();
                user.setEmail(email);
                user.setPassword(UUID.randomUUID().toString());
                user.setUserName(name.replaceAll("\\s+", "_"));

                UserDTO userDTO = new UserDTO();
                userDTO.setEmail(user.getEmail());
                userDTO.setUserName(user.getUserName());
                userDTO.setPassword(user.getPassword());

                userService.createUser(userDTO);



            }

            String token = jwtUtils.generateToken(user.getUserName());
            JwtResponseG jwtResponseG = new JwtResponseG(token, user.getUserName(), email);


            return ResponseEntity.ok(jwtResponseG);



        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Token verification failed.");
        }
    }
}

