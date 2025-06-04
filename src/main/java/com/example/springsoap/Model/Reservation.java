package com.example.springsoap.Model;

import java.util.ArrayList;
import java.util.List;

public class Reservation {
    List<RoomReservation> roomReservations;
    List<FlightReservation> flightReservations;

    public Reservation() {
        roomReservations = new ArrayList<RoomReservation>();
        flightReservations = new ArrayList<FlightReservation>();
    }

    public List<RoomReservation> getRoomReservations() {
        return roomReservations;
    }

    public List<FlightReservation> getFlightReservations() {
        return flightReservations;
    }

    public void addRoomReservation(RoomReservation roomReservation) {
        roomReservations.add(roomReservation);
    }

    public void addFlightReservation(FlightReservation flightReservation) {
        flightReservations.add(flightReservation);
    }

    public void clear() {
        roomReservations.clear();
        flightReservations.clear();
    }

}
