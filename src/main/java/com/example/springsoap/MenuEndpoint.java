package com.example.springsoap;

import java.util.List;
import java.util.ArrayList;

import io.foodmenu.gt.webservice.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;


@Endpoint
public class MenuEndpoint {
    private static final String NAMESPACE_URI = "http://foodmenu.io/gt/webservice";

    private RoomRepository roomrepo;

    @Autowired
    public MenuEndpoint(RoomRepository roomrepo) {
        this.roomrepo = roomrepo;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getRoomRequest")
    @ResponsePayload
    public GetRoomResponse getRoom(@RequestPayload GetRoomRequest request) {
        GetRoomResponse response = new GetRoomResponse();
        response.setRoom(roomrepo.findRoom(request.getNumber()));

        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getFreeRoomsRequest")
    @ResponsePayload
    public GetFreeRoomsResponse getFreeRooms(@RequestPayload GetFreeRoomsRequest request) {
        List<Room> freeRooms = new ArrayList<>();

        for (Room room : roomrepo.findAllRooms()) {
            if (!roomrepo.isRoomBooked(room, request.getStartDate(), request.getEndDate())) {
                freeRooms.add(room);
            }
        }

        GetFreeRoomsResponse response = new GetFreeRoomsResponse();
        response.getRooms().addAll(freeRooms);

        return response;
    }


    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "addBookingRequest")
    @ResponsePayload
    public AddBookingResponse addOrder(@RequestPayload AddBookingRequest request) {
        AddBookingResponse response = new AddBookingResponse();
        response.setBooking(roomrepo.addBooking(request.getRoomNumber(), request.getStartDate(), request.getEndDate()));

        return response;
    }
}