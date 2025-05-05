package com.example.springsoap;

import javax.annotation.PostConstruct;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.HashMap;
import java.util.Map;


import io.foodmenu.gt.webservice.*;


import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class RoomRepository {
    private static final Map<Integer, Room> rooms = new HashMap<Integer, Room>();

    @PostConstruct
    public void initData() {

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
    }

    public Room findRoom(int number) {
        Assert.notNull(number, "The room's number must not be null");
        return rooms.get(number);
    }

    /*
    public Room findBiggestMeal() {

        if (rooms == null) return null;
        if (rooms.size() == 0) return null;

        var values = rooms.values();
        return values.stream().max(Comparator.comparing(Room::getKcal)).orElseThrow(NoSuchElementException::new);

    }*/

    public Booking addBooking(int roomNumber, XMLGregorianCalendar startDate, XMLGregorianCalendar endDate) {
        Booking order = new Booking();
        order.setRoom(findRoom(roomNumber));
        order.setStartDate(startDate);
        order.setEndDate(endDate);

        return order;
    }

}