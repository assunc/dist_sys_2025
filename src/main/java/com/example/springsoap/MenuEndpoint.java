package com.example.springsoap;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

import com.example.springsoap.Entities.Room;
import com.example.springsoap.Entities.Booking;
import com.example.springsoap.Repositories.BookingRepository;
import com.example.springsoap.Repositories.RoomRepository;
import io.foodmenu.gt.webservice.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import javax.swing.text.html.parser.Entity;


@Endpoint
public class MenuEndpoint {
    private static final String NAMESPACE_URI = "http://foodmenu.io/gt/webservice";

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private HotelServices hotelServices;

//    @Autowired
//    public MenuEndpoint(HotelServices hotelServices) {
//        this.hotelServices = hotelServices;
//    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getRoomRequest")
    @ResponsePayload
    public GetRoomResponse getRoom(@RequestPayload GetRoomRequest request) {
        GetRoomResponse response = new GetRoomResponse();
        Optional<Room> room = roomRepository.findById(request.getNumber());
        if (room.isPresent()) {
            RoomXml roomXml = new RoomXml();
            roomXml.setNumber(room.get().getNumber());
            roomXml.setNOfPeople(room.get().getPeople());
            roomXml.setPrice(room.get().getPrice().intValue());
            response.setRoom(roomXml);
        }

        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getFreeRoomsRequest")
    @ResponsePayload
    public GetFreeRoomsResponse getFreeRooms(@RequestPayload GetFreeRoomsRequest request) {
        List<RoomXml> freeRooms = new ArrayList<>();

        for (Room room : roomRepository.findAll()) {
            if (!hotelServices.isRoomBooked(room, request.getStartDate(), request.getEndDate())) {
                RoomXml roomXml = new RoomXml();
                roomXml.setNumber(room.getNumber());
                roomXml.setNOfPeople(room.getPeople());
                roomXml.setPrice(room.getPrice().intValue());
                freeRooms.add(roomXml);
            }
        }

        GetFreeRoomsResponse response = new GetFreeRoomsResponse();
        response.getRooms().addAll(freeRooms);

        return response;
    }


    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "addBookingRequest")
    @ResponsePayload
    public AddBookingResponse addBooking(@RequestPayload AddBookingRequest request) {
        AddBookingResponse response = new AddBookingResponse();
        for (int roomNumber : request.getRoomNumber()) {
            Booking booking = hotelServices.addBooking(roomNumber, request.getStartDate(), request.getEndDate());
            if (booking != null) {
                response.getBookingId().add(booking.getId());
                response.getStatus().add(BookingStatus.fromValue(booking.getStatus()));
            } else {
                response.getBookingId().add(-1);
                response.getStatus().add(BookingStatus.NOT_AVAILABLE);
            }
        }
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "confirmBookingRequest")
    @ResponsePayload
    public ConfirmBookingResponse confirmBooking(@RequestPayload ConfirmBookingRequest request) {
        ConfirmBookingResponse response = new ConfirmBookingResponse();
        for (int bookingId : request.getBookingId()) {
            Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                    () -> new IllegalArgumentException("Booking Id not found: " + bookingId));
            if (booking.getStatus().equals(BookingStatus.PENDING.toString())) {
                booking.setStatus(String.valueOf(BookingStatus.RESERVED));
                bookingRepository.save(booking);
            }
        }
        response.setStatus(BookingStatus.RESERVED);
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "cancelBookingRequest")
    @ResponsePayload
    public CancelBookingResponse cancelBooking(@RequestPayload CancelBookingRequest request) {
        CancelBookingResponse response = new CancelBookingResponse();
        for (int bookingId : request.getBookingId()) {
            Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                    () -> new IllegalArgumentException("Booking Id not found: " + bookingId));
            booking.setStatus(String.valueOf(BookingStatus.CANCELED));
            bookingRepository.save(booking);
        }
        response.setStatus(BookingStatus.CANCELED);
        return response;
    }
}