package com.example.springsoap.Model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Reservation {
    private List<RoomReservation> rooms;
    private List<FlightReservation> flights;

    public Reservation() {
        rooms = new ArrayList<>();
        flights = new ArrayList<>();
    }

    public void addRoom(int roomId, Date startDate, Date endDate) {
        rooms.add(new RoomReservation(roomId, startDate, endDate));
    }

    public void addFlight(int flightId) {
        flights.add(new FlightReservation(flightId));
    }

    public List<RoomReservation> getRooms() {
        return rooms;
    }

    public List<FlightReservation> getFlights() {
        return flights;
    }
}
