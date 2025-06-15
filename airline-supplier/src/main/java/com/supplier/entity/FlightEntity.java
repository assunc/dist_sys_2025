package com.supplier.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "flights")
public class FlightEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer flightNumber;

    private String planeModel;
    private String source;
    private String destination;
    private Timestamp departureTime;
    private Timestamp arrivalTime;
    private BigDecimal priceEconomy;
    private BigDecimal priceBusiness;
    private BigDecimal priceFirst;

    // Getters and Setters
    public Integer getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(Integer flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getPlaneModel() {
        return planeModel;
    }

    public void setPlaneModel(String planeModel) {
        this.planeModel = planeModel;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Timestamp getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(Timestamp departureTime) {
        this.departureTime = departureTime;
    }

    public Timestamp getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(Timestamp arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public BigDecimal getPriceEconomy() {
        return priceEconomy;
    }

    public void setPriceEconomy(BigDecimal priceEconomy) {
        this.priceEconomy = priceEconomy;
    }

    public BigDecimal getPriceBusiness() {
        return priceBusiness;
    }

    public void setPriceBusiness(BigDecimal priceBusiness) {
        this.priceBusiness = priceBusiness;
    }

    public BigDecimal getPriceFirst() {
        return priceFirst;
    }

    public void setPriceFirst(BigDecimal priceFirst) {
        this.priceFirst = priceFirst;
    }
}
