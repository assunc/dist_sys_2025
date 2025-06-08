package com.example.springsoap.Model;

public class FlightReservation {
    private final int flightId;
    private final String seatNumber;

    public FlightReservation(int flightId, String seatNumber) {
        this.flightId = flightId;
        this.seatNumber = seatNumber;
    }

    public int getFlightId() {
        return flightId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }
}
