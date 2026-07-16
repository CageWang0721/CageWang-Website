package com.example.blog.auth.model;

public class UserAccount {

    private Long id;
    private String username;
    private String passwordHash;
    private String nickname;
    private String email;
    private String role;
    private String status;

    public Long id() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String username() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String nickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String email() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String role() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean enabled() {
        return "ACTIVE".equals(status);
    }
}
