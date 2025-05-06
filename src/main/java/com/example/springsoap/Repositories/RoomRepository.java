package com.example.springsoap.Repositories;

import com.example.springsoap.Entities.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Integer> {
}