package com.example.springsoap.Model;

// Define a simple Room class as POJO to hold parsed data
public class Room {
    private int id;
    private int number;
    private int nOfPeople;
    private int price;

    public Room(int id, int number, int nOfPeople, int price) {
        this.id = id;
        this.number = number;
        this.nOfPeople = nOfPeople;
        this.price = price;
    }

    // Getters for Thymeleaf

    public int getId() {
        return id;
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
