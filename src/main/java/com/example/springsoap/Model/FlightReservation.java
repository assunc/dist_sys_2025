package com.example.springsoap.Model;

public class FlightReservation {
    private final int flightNumber;
    private final String seatNumber;

    public FlightReservation(int flightNumber, String seatNumber) {
        this.flightNumber = flightNumber;
        this.seatNumber = seatNumber;
    }

    public int getFlightNumber() {
        return flightNumber;
    }

    public String getSeatNumber() {
        return seatNumber;
    }
}
