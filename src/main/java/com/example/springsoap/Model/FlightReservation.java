package com.example.springsoap.Model;

public class FlightReservation {
    private int flightId;

    public FlightReservation(int flightId) {
        this.flightId = flightId;
    }

    public int getFlightId() {
        return flightId;
    }

    public void setFlightId(int flightId) {
        this.flightId = flightId;
    }
}
