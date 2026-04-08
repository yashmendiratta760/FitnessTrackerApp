package com.yash.Fitness.Tracker.DTO;

public class JwtResponse {

    private String token;

    // Constructor
    public JwtResponse(String token) {
        this.token = token;
    }

    // Getter
    public String getToken() {
        return token;
    }

    // Setter
    public void setToken(String token) {
        this.token = token;
    }
}

