package com.example.springsoap.Model;


public class OrderProcessingMessage implements java.io.Serializable {
    private int orderId;
    private Reservation reservation;

    public OrderProcessingMessage(int orderId, Reservation reservation) {
        this.orderId = orderId;
        this.reservation = reservation;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }
}