package com.example.springsoap.Repositories;

import com.example.springsoap.Entities.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
}
