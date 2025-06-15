package com.example.springsoap.Model;

public class FlightReservation {
    private final Flight flight;
    private final String seatNumber;
    private final Long seatId;

    public FlightReservation(Flight flight, String seatNumber, Long seatId) {
        this.flight = flight;
        this.seatNumber = seatNumber;
        this.seatId = seatId;
    }

    public Flight getFlight() {
        return flight;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public Long getSeatId() {
        return seatId;
    }

    @Override
    public String toString() {
        return "FlightReservation{" +
                "flight=" + flight +
                ", seatNumber='" + seatNumber + '\'' +
                ", seatId=" + seatId +
                '}';
    }
}
