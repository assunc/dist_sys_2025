package com.example.springsoap;

import com.example.springsoap.Entities.Room;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.example.springsoap.Repositories.RoomRepository;

import java.util.List;

@Component
public class DatabaseConnectionTest implements CommandLineRunner {

    private final RoomRepository roomRepository;

    public DatabaseConnectionTest(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public void run(String... args) {
        System.out.println("🔍 Testing DB connection...");

        List<Room> rooms = roomRepository.findAll();
        System.out.println(" Found " + rooms.size() + " rooms in the database.");

        rooms.forEach(room -> {
            System.out.println("Room number: " + room.getNumber() +
                    ", People: " + room.getPeople() +
                    ", Price: " + room.getPrice());
        });
    }
}
