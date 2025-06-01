package com.example.springsoap;

import com.example.springsoap.Entities.Booking;
import com.example.springsoap.Entities.Room;

import com.example.springsoap.Repositories.BookingRepository;
import com.example.springsoap.Repositories.RoomRepository;

import javax.xml.datatype.XMLGregorianCalendar;

import io.foodmenu.gt.webservice.BookingStatus;
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
            if ((booking.getStatus().equals(BookingStatus.RESERVED.toString()) || booking.getStatus().equals(BookingStatus.PENDING.toString())) &&
                    booking.getRoom().getId().compareTo(room.getId()) == 0 &&
                    localDateToXMLGC(booking.getStartDate()).toGregorianCalendar().compareTo(end.toGregorianCalendar()) < 0 &&
                    localDateToXMLGC(booking.getEndDate()).toGregorianCalendar().compareTo(start.toGregorianCalendar()) > 0) {
                return true;
            }
        }
        return false;
    }

    public Booking addBooking(int roomId, XMLGregorianCalendar startDate, XMLGregorianCalendar endDate) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room id not found: " + roomId));
        if (!isRoomBooked(room, startDate, endDate)) {
            Booking booking = new Booking();
            booking.setRoom(room);
            booking.setStartDate(XMLGCtoLocalDate(startDate));
            booking.setEndDate(XMLGCtoLocalDate(endDate));
            booking.setStatus("Pending");
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
