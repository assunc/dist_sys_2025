package com.example.springsoap.Entities;

import jakarta.persistence.*;

@Entity
@Table(name = "flight_orders")
public class FlightOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order; // FK to Order table

    @Column(name = "flight_number", length = 50, nullable = false)
    private String flightNumber; // Optional: could be from user input or seat metadata

    @Column(name = "seat_number", length = 5, nullable = false)
    private String seatNumber;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId; // 🔄 Renamed from reservationId

    @Column(name = "status", length = 500)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airlinesupplier_id") // optional FK
    private AirlineSupplier airlineSupplier;

    // ---------- Constructors ----------
    public FlightOrder() {
    }

    public FlightOrder(Order order, String flightNumber, String seatNumber, Long bookingId, String status, AirlineSupplier airlineSupplier) {
        this.order = order;
        this.flightNumber = flightNumber;
        this.seatNumber = seatNumber;
        this.bookingId = bookingId;
        this.status = status;
        this.airlineSupplier = airlineSupplier;
    }

    // ---------- Getters and Setters ----------
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

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
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
                ", flightNumber='" + flightNumber + '\'' +
                ", seatNumber='" + seatNumber + '\'' +
                ", bookingId=" + bookingId +
                ", status='" + status + '\'' +
                ", airlineSupplier=" + (airlineSupplier != null ? airlineSupplier.getName() : "null") +
                '}';
    }
}
