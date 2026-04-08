package com.yash.Fitness.Tracker.DTO;

public class JwtResponseG {
    private String token;
    private String userName;
    private String email;

    public JwtResponseG(String token, String userName, String email) {
        this.token = token;
        this.userName = userName;
        this.email = email;
    }

    // Getters and setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
