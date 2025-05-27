package com.supplier.repository;

import com.supplier.entity.FlightEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FlightRepository extends JpaRepository<FlightEntity, Long> {

    List<FlightEntity> findBySourceAndDestination(String source, String destination);

}
