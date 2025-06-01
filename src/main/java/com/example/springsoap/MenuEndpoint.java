package com.example.springsoap;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import com.example.springsoap.Entities.Room;
import com.example.springsoap.Entities.Booking;
import com.example.springsoap.Entities.Hotelinfo;
import com.example.springsoap.Repositories.BookingRepository;
import com.example.springsoap.Repositories.RoomRepository;
import com.example.springsoap.Repositories.HotelinfoRepository;
import io.foodmenu.gt.webservice.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;


@Endpoint
public class MenuEndpoint {
    private static final String NAMESPACE_URI = "http://foodmenu.io/gt/webservice";

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private HotelinfoRepository hotelInfoRepository;

    @Autowired
    private HotelServices hotelServices;

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getRoomRequest")
    @ResponsePayload
    public GetRoomResponse getRoom(@RequestPayload GetRoomRequest request) {
        GetRoomResponse response = new GetRoomResponse();
        Optional<Room> room = roomRepository.findById(request.getRoomId());
        if (room.isPresent()) {
            RoomXml roomXml = new RoomXml();
            roomXml.setRoomId(room.get().getId());
            roomXml.setHotelId(room.get().getHotelId());
            roomXml.setNumber(room.get().getNumber());
            roomXml.setNOfPeople(room.get().getPeople());
            roomXml.setPrice(room.get().getPrice().intValue());
            response.setRoom(roomXml);
        }

        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getHotelRequest")
    @ResponsePayload
    public GetHotelResponse getRoom(@RequestPayload GetHotelRequest request) {
        GetHotelResponse response = new GetHotelResponse();
        Optional<Hotelinfo> hotel = hotelInfoRepository.findById(request.getHotelId());
        if (hotel.isPresent()) {
            HotelInfoXml hotelXml = new HotelInfoXml();
            hotelXml.setId(hotel.get().getId());
            hotelXml.setName(hotel.get().getName());
            hotelXml.setAddress(hotel.get().getAddress());
            hotelXml.setCity(hotel.get().getCity());
            hotelXml.setCountry(hotel.get().getCountry());
            hotelXml.setPhoneNumber(hotel.get().getPhoneNumber());
            hotelXml.setDescription(hotel.get().getDescription());
            response.setHotel(hotelXml);
        }

        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getFreeRoomsRequest")
    @ResponsePayload
    public GetFreeRoomsResponse getFreeRooms(@RequestPayload GetFreeRoomsRequest request) {
        List<RoomXml> freeRooms = new ArrayList<>();

        for (Room room : roomRepository.findAllByHotelId(request.getHotelId())) {
            if (!hotelServices.isRoomBooked(room, request.getStartDate(), request.getEndDate())) {
                RoomXml roomXml = new RoomXml();
                roomXml.setRoomId(room.getId());
                roomXml.setHotelId(room.getHotelId());
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

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getFreeHotelsRequest")
    @ResponsePayload
    public GetFreeHotelsResponse getFreeHotels(@RequestPayload GetFreeHotelsRequest request) {
        List<HotelInfoXml> freeHotels = new ArrayList<>();

        for (Hotelinfo hotel : hotelInfoRepository.findAll()) {
            for (Room room : roomRepository.findAllByHotelId(hotel.getId())) {
                if (!hotelServices.isRoomBooked(room, request.getStartDate(), request.getEndDate())) {
                    HotelInfoXml hotelXml = new HotelInfoXml();
                    hotelXml.setId(hotel.getId());
                    hotelXml.setName(hotel.getName());
                    hotelXml.setAddress(hotel.getAddress());
                    hotelXml.setCity(hotel.getCity());
                    hotelXml.setCountry(hotel.getCountry());
                    hotelXml.setPhoneNumber(hotel.getPhoneNumber());
                    hotelXml.setDescription(hotel.getDescription());

                    freeHotels.add(hotelXml);
                    break;
                }
            }
        }

        GetFreeHotelsResponse response = new GetFreeHotelsResponse();
        response.getHotels().addAll(freeHotels);

        return response;
    }


    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "addBookingRequest")
    @ResponsePayload
    public AddBookingResponse addBooking(@RequestPayload AddBookingRequest request) {
        AddBookingResponse response = new AddBookingResponse();
        for (int roomId : request.getRoomId()) {
            Booking booking = hotelServices.addBooking(roomId, request.getStartDate(), request.getEndDate());
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
                booking.setStatus(String.valueOf(BookingStatus.BOOKED));
                bookingRepository.save(booking);
            }
        }
        response.setStatus(BookingStatus.BOOKED);
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