package com.example.springsoap.Repositories;

import com.example.springsoap.Entities.Booking;
import com.example.springsoap.Entities.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
    List<Booking> findAllByRoomId(Integer roomId);
}
