package com.supplier.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "seats")  // maps to "seats" table in the DB
public class SeatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seatId;

    private Integer flightNumber;  // foreign key to flights table
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    private SeatClass seatClass;

    private Boolean available;

    // Enum for seat class
    public enum SeatClass {
        economy, business, first
    }

    // Getters and Setters
    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public Integer getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(Integer flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public SeatClass getSeatClass() {
        return seatClass;
    }

    public void setSeatClass(SeatClass seatClass) {
        this.seatClass = seatClass;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }


}
