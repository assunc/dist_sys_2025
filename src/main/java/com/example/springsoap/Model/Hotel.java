package com.example.springsoap.Model;

public class Hotel {
    private int id;
    private String name;
    private String address;
    private String city;
    private String country;
    private String phoneNumber;
    private String Description;
    private String imageUrl;

    public Hotel(int id, String name, String address, String city, String country, String phoneNumber, String description, String imageUrl) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.city = city;
        this.country = country;
        this.phoneNumber = phoneNumber;
        Description = description;
        this.imageUrl = imageUrl;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getDescription() {
        return Description;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
