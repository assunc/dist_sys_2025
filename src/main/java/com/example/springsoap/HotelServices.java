package com.example.springsoap;

import com.example.springsoap.Entities.Booking;
import com.example.springsoap.Entities.Room;

import com.example.springsoap.Repositories.BookingRepository;
import com.example.springsoap.Repositories.RoomRepository;

import javax.xml.datatype.XMLGregorianCalendar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

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
        Optional<Room> room = roomRepository.findById(roomNumber);
        if (room.isPresent()) {
            Booking booking = new Booking();
            booking.setRoom(room.get());
            booking.setStartDate(XMLGCtoLocalDate(startDate));
            booking.setEndDate(XMLGCtoLocalDate(endDate));
            bookingRepository.save(booking);
            return booking;
        }
        return null;
    }

    public XMLGregorianCalendar localDateToXMLGC(LocalDate localDate) {
        return DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(GregorianCalendar.from(localDate.atStartOfDay(ZoneId.systemDefault())));
    }

    public static LocalDate XMLGCtoLocalDate(XMLGregorianCalendar xmlDate) {
        return xmlDate.toGregorianCalendar().toZonedDateTime().toLocalDate();
    }
}
