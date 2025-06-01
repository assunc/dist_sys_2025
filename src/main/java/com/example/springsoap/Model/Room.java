package com.example.springsoap.Model;

// Define a simple Room class as POJO to hold parsed data
public class Room {
    private int hotelId;
    private String hotelName;
    private int number;
    private int nOfPeople;
    private int price;

    public Room(int hotelId, String hotelName, int number, int nOfPeople, int price) {
        this.hotelId = hotelId;
        this.hotelName = hotelName;
        this.number = number;
        this.nOfPeople = nOfPeople;
        this.price = price;
    }

    // Getters for Thymeleaf
    public int getHotelId() {
        return hotelId;
    }

    public String getHotelName() {
        return hotelName;
    }

    public int getNumber() {
        return number;
    }

    public int getNOfPeople() {
        return nOfPeople;
    }

    public int getPrice() {
        return price;
    }
}
