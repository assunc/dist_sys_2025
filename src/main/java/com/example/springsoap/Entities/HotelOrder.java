package com.example.springsoap.Entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "hotel_orders")
public class HotelOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "booking_id", nullable = false)
    private Integer bookingId;

    @Column(name = "status", length = 500)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotelsupplier_id")
    private HotelSupplier hotelSupplier; // Foreign key to HotelSupplier entity

    @Column(name = "room_id")
    private Integer roomId;

    @Column(name = "hotel_name")
    private String hotelName;

    @Column(name = "room_number")
    private int roomNumber;

    // Constructors
    public HotelOrder() {
    }

    public HotelOrder(Order order, LocalDate startDate, LocalDate endDate, Integer bookingId, String status, HotelSupplier hotelSupplier, Integer roomId, String hotelName, int roomNumber) {
        this.order = order;
        this.startDate = startDate;
        this.endDate = endDate;
        this.bookingId = bookingId;
        this.status = status;
        this.hotelSupplier = hotelSupplier;
        this.roomId = roomId;
        this.hotelName = hotelName;
        this.roomNumber = roomNumber;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getBookingId() {
        return bookingId;
    }

    public void setBookingId(Integer bookingId) {
        this.bookingId = bookingId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public HotelSupplier getHotelSupplier() {
        return hotelSupplier;
    }

    public void setHotelSupplier(HotelSupplier hotelSupplier) {
        this.hotelSupplier = hotelSupplier;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public void setRooNumber(int roomNumber) {
        this.roomNumber = roomNumber;
    }

    @Override
    public String toString() {
        return "HotelOrder{" +
                "id=" + id +
                ", order=" + order +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", bookingId=" + bookingId +
                ", status='" + status + '\'' +
                ", hotelSupplier=" + hotelSupplier +
                ", roomId=" + roomId +
                ", hotelName='" + hotelName + '\'' +
                ", roomNumber=" + roomNumber +
                '}';
    }
}