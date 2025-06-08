package com.example.springsoap.Model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public Room(String roomInfo) {
        Pattern pattern = Pattern.compile("Room\\{id=(\\d+), number=(\\d+), nOfPeople=(\\d+), price=(\\d+)\\}");
        Matcher matcher = pattern.matcher(roomInfo);

        if (matcher.matches()) {
            try {
                this.id = Integer.parseInt(matcher.group(1));
                this.number = Integer.parseInt(matcher.group(2));
                this.nOfPeople = Integer.parseInt(matcher.group(3));
                this.price = Integer.parseInt(matcher.group(4));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number format found in room string: " + roomInfo, e);
            }
        } else {
            throw new IllegalArgumentException("Invalid room string format. Expected 'Room{id=X, number=Y, nOfPeople=Z, price=W}': " + roomInfo);
        }
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

    @Override
    public String toString() {
        return "Room{" +
                "id=" + id +
                ", number=" + number +
                ", nOfPeople=" + nOfPeople +
                ", price=" + price +
                '}';
    }
}
