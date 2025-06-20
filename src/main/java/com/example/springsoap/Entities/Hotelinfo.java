package com.example.springsoap.Entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Hotelinfo {

    @Id
    private Integer id;

    private String name;

    private String address;

    private String city;

    private String country;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String description;

    @Column(name = "image")
    private String imageURL;

    // Getters and setters

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageURL;
    }

    public void setImageUrl(String imageURL) {
        this.imageURL = imageURL;
    }
}
