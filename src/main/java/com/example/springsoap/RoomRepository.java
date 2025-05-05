package com.example.springsoap;

import javax.annotation.PostConstruct;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;


import io.foodmenu.gt.webservice.*;


import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class RoomRepository {
    private static final Map<Integer, Room> rooms = new HashMap<Integer, Room>();
    private static final List<Booking> bookings = new ArrayList<>();

    @PostConstruct
    public void initData() {

        DatatypeFactory dataFactory = DatatypeFactory.newDefaultInstance();

        Room a = new Room();
        a.setNumber(101);
        a.setNOfPeople(2);
        a.setPrice(100);

        rooms.put(a.getNumber(), a);

        Room b = new Room();
        b.setNumber(102);
        b.setNOfPeople(2);
        b.setPrice(100);


        rooms.put(b.getNumber(), b);

        Room c = new Room();
        c.setNumber(201);
        c.setNOfPeople(1);
        c.setPrice(50);

        rooms.put(c.getNumber(), c);

        Booking d = new Booking();
        d.setRoom(a);
        d.setStartDate(dataFactory.newXMLGregorianCalendarDate(2025, 5, 6, 0));
        d.setEndDate(dataFactory.newXMLGregorianCalendarDate(2025, 5, 10, 0));

        bookings.add(d);
    }

    public Room findRoom(int number) {
        return rooms.get(number);
    }

    public List<Room> findAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public List<Booking> findAllBookings() {
        return bookings;
    }

    public Booking addBooking(int roomNumber, XMLGregorianCalendar startDate, XMLGregorianCalendar endDate) {
        Booking booking = new Booking();
        booking.setRoom(findRoom(roomNumber));
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);

        bookings.add(booking);
        return booking;
    }

    public boolean isRoomBooked(Room room, XMLGregorianCalendar start, XMLGregorianCalendar end) {
        for (Booking booking : bookings) {
            if (booking.getRoom().getNumber() == room.getNumber() &&
                    booking.getStartDate().toGregorianCalendar().compareTo(end.toGregorianCalendar()) < 0 &&
                    booking.getEndDate().toGregorianCalendar().compareTo(start.toGregorianCalendar()) > 0) {
                return true;
            }
        }
        return false;
    }
}