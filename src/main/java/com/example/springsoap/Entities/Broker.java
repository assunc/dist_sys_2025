package com.example.springsoap.Entities;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "broker")
public class Broker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "hashed_password", nullable = false, length = 255)
    private String hashedPassword;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
