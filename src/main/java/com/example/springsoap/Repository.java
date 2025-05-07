package com.example.springsoap;

import javax.annotation.PostConstruct;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import io.foodmenu.gt.webservice.*;


import org.springframework.stereotype.Component;

@Component
public class Repository {
    private static final Map<Integer, RoomXml> rooms = new HashMap<>();
    private static final Map<Integer, BookingXml> bookings = new HashMap<>();

    @PostConstruct
    public void initData() {

        DatatypeFactory dataFactory = DatatypeFactory.newDefaultInstance();

        RoomXml a = new RoomXml();
        a.setNumber(101);
        a.setNOfPeople(2);
        a.setPrice(100);

        rooms.put(a.getNumber(), a);

        RoomXml b = new RoomXml();
        b.setNumber(102);
        b.setNOfPeople(2);
        b.setPrice(100);


        rooms.put(b.getNumber(), b);

        RoomXml c = new RoomXml();
        c.setNumber(201);
        c.setNOfPeople(1);
        c.setPrice(50);

        rooms.put(c.getNumber(), c);

        BookingXml d = new BookingXml();
        d.setId(1);
        d.setRoomNumber(a.getNumber());
        d.setStartDate(dataFactory.newXMLGregorianCalendarDate(2025, 5, 6, 0));
        d.setEndDate(dataFactory.newXMLGregorianCalendarDate(2025, 5, 10, 0));

        bookings.put(d.getId(), d);
    }

    public RoomXml findRoom(int number) {
        return rooms.get(number);
    }

    public List<RoomXml> findAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public BookingXml findBooking(int id) { return bookings.get(id); }

    public List<BookingXml> findAllBookings() {
        return new ArrayList<BookingXml>(bookings.values());
    }

}