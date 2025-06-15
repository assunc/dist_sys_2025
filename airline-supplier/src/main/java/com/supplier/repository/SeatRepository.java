package com.supplier.repository;

import com.supplier.entity.SeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeatRepository extends JpaRepository<SeatEntity, Long> {

    // Custom query for getting available seats of a flight
    //List<SeatEntity> findByFlightNumberAndAvailableTrue(Long flightNumber);
    List<SeatEntity> findByFlightNumberAndAvailableTrueAndSeatClass(Long flightNumber, SeatEntity.SeatClass seatClass);

    // You can add more custom queries as needed!
}
