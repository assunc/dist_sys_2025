package com.example.springsoap.Repository;

import com.example.springsoap.Entities.FlightOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightOrderRepository extends JpaRepository<FlightOrder, Integer> {
}
