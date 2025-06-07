package com.example.springsoap.Entities;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "auth0_id", nullable = false, unique = true)
    private String auth0Id;

    private String email;

    private String name;

    @Column(name = "payment_info")
    private String paymentInfo;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getAuth0Id() { return auth0Id; }
    public void setAuth0Id(String auth0Id) { this.auth0Id = auth0Id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPaymentInfo() {
        return paymentInfo;
    }

    public void setPaymentInfo(String paymentInfo) {
        this.paymentInfo = paymentInfo;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", auth0Id='" + auth0Id + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", paymentInfo='" + paymentInfo + '\'' +
                ", deliveryAddress='" + deliveryAddress + '\'' +
                '}';
    }
}