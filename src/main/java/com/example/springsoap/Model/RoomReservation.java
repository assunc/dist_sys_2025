package com.example.springsoap.Model;

import java.util.Date;

public class RoomReservation {
    private final int roomId;
    private final Date startDate;
    private final Date endDate;

    public RoomReservation(int roomId, Date startDate, Date endDate) {
        this.roomId = roomId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public int getRoomId() {
        return roomId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }
}
