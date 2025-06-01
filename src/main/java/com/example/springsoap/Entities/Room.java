package com.example.springsoap.Entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.math.BigDecimal;

@Entity
public class Room {

    @Id
    private Integer id;

    private Integer number;

    private Integer people;

    private BigDecimal price;

    @Column(name = "hotel_id")
    private Integer hotelId;

    // Getters and setters

    public Integer getId() {
        return id;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Integer getPeople() {
        return people;
    }

    public void setPeople(Integer people) {
        this.people = people;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getHotelId() {
        return hotelId;
    }

    public void setNHotelId(Integer hotelId) {
        this.hotelId = hotelId;
    }
}
