package com.example.springsoap.Repositories;

import com.example.springsoap.Entities.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Integer> {
    Optional<Room> findByNumber(Integer number);
}