package com.example.springsoap.Repositories;

import com.example.springsoap.Entities.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Integer> {
    List<Room> findAllByHotelId(Integer hotelId);
}