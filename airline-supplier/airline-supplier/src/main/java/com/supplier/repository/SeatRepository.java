package com.supplier.repository;

import com.supplier.entity.SeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeatRepository extends JpaRepository<SeatEntity, Long> {

    List<SeatEntity> findByFlightNumberAndAvailableTrue(Long flightNumber);

}
