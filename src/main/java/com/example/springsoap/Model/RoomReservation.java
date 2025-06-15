package com.example.springsoap.Model;

import java.util.Date;

public class RoomReservation {
    private final Room room;
    private final String hotelName;
    private final Date startDate;
    private final Date endDate;

    public RoomReservation(Room room, String hotelName, Date startDate, Date endDate) {
        this.room = room;
        this.hotelName = hotelName;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Room getRoom() {
        return room;
    }

    public String getHotelName() {
        return hotelName;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    @Override
    public String toString() {
        return "RoomReservation{" +
                "room=" + room +
                ", hotelName='" + hotelName + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}