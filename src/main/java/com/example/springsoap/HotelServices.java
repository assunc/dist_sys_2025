package com.example.springsoap;

import com.example.springsoap.Entities.Booking;
import com.example.springsoap.Entities.Room;

import com.example.springsoap.Repositories.BookingRepository;
import com.example.springsoap.Repositories.RoomRepository;

import javax.xml.datatype.XMLGregorianCalendar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeFactory;

@Service
public class HotelServices {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    public boolean isRoomBooked(Room room, XMLGregorianCalendar start, XMLGregorianCalendar end) {
        for (Booking booking : bookingRepository.findAll()) {
            if (booking.getRoom().getNumber().compareTo(room.getNumber()) == 0 &&
                    localDateToXMLGC(booking.getStartDate()).toGregorianCalendar().compareTo(end.toGregorianCalendar()) < 0 &&
                    localDateToXMLGC(booking.getEndDate()).toGregorianCalendar().compareTo(start.toGregorianCalendar()) > 0) {
                return true;
            }
        }
        return false;
    }

    public Booking addBooking(int roomNumber, XMLGregorianCalendar startDate, XMLGregorianCalendar endDate) {
        Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new IllegalArgumentException("Room number not found: " + roomNumber));

        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setStartDate(XMLGCtoLocalDate(startDate));
        booking.setEndDate(XMLGCtoLocalDate(endDate));
        booking.setStatus("Pending");
        bookingRepository.save(booking);
        return booking;
    }

    public XMLGregorianCalendar localDateToXMLGC(LocalDate localDate) {
        return DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(GregorianCalendar.from(localDate.atStartOfDay(ZoneId.systemDefault())));
    }

    public static LocalDate XMLGCtoLocalDate(XMLGregorianCalendar xmlDate) {
        return xmlDate.toGregorianCalendar().toZonedDateTime().toLocalDate();
    }
}
