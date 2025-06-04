package com.example.springsoap.Entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders") // Map to the 'orders' table
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ManyToOne relationship with the User entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Maps to the 'user_id' column in the orders table
    private User user; // The associated User

    @Column(name = "delivery_addr", columnDefinition = "TEXT", nullable = false)
    private String deliveryAddress; // Mapped from 'delivery_addr'

    @Column(name = "payment_info", columnDefinition = "TEXT")
    private String paymentInfo; // Mapped from 'payment_info'

    @Column(name = "status", length = 500)
    private String status;

    @CreationTimestamp // Automatically set the timestamp on creation
    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt; // Mapped from 'created_at'

    // Constructors
    public Order() {
    }

    public Order(User user, String deliveryAddress, String paymentInfo, String status) {
        this.user = user;
        this.deliveryAddress = deliveryAddress;
        this.paymentInfo = paymentInfo;
        this.status = status;
        // createdAt is handled by @CreationTimestamp
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getPaymentInfo() {
        return paymentInfo;
    }

    public void setPaymentInfo(String paymentInfo) {
        this.paymentInfo = paymentInfo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", user=" + (user != null ? user.getName() : "null") +
                ", deliveryAddress='" + deliveryAddress + '\'' +
                ", paymentInfo='" + paymentInfo + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}