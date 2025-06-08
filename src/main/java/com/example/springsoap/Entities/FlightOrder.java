package com.example.springsoap.Entities;

import jakarta.persistence.*;

@Entity
@Table(name = "flight_orders")
public class FlightOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false) // Maps to the 'order_id' column
    private Order order; // The associated Order

    @Column(name = "flight_number", length = 50, nullable = false)
    private String flightNumber; // Foreign key to Flight entity

    @Column(name = "seat_number", length = 5, nullable = false)
    private String seatNumber;

    @Column(name = "reservation_id", nullable = false)
    private Integer reservationId;

    @Column(name = "status", length = 500)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airlinesupplier_id") // nullable = true based on DB schema
    private AirlineSupplier airlineSupplier; // Foreign key to AirlineSupplier entity

    // Constructors
    public FlightOrder() {
    }

    public FlightOrder(Order order, String flightNumber, String seatNumber, Integer reservationId, String status, AirlineSupplier airlineSupplier) {
        this.order = order;
        this.flightNumber = flightNumber;
        this.seatNumber = seatNumber;
        this.reservationId = reservationId;
        this.status = status;
        this.airlineSupplier = airlineSupplier;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Integer getReservationId() {
        return reservationId;
    }

    public void setReservationId(Integer reservationId) {
        this.reservationId = reservationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public AirlineSupplier getAirlineSupplier() {
        return airlineSupplier;
    }

    public void setAirlineSupplier(AirlineSupplier airlineSupplier) {
        this.airlineSupplier = airlineSupplier;
    }

    @Override
    public String toString() {
        return "FlightOrder{" +
                "id=" + id +
                ", order=" + (order != null ? order.getId() : "null") +
                ", flight='" + flightNumber + '\'' +
                ", seatNumber='" + seatNumber + '\'' +
                ", reservationId=" + reservationId +
                ", status='" + status + '\'' +
                ", airlineSupplier=" + (airlineSupplier != null ? airlineSupplier.getName() : "null") +
                '}';
    }
}
