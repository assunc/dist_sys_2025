package com.example.springsoap.Repository;

import com.example.springsoap.Entities.FlightOrder;
import com.example.springsoap.Entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlightOrderRepository extends JpaRepository<FlightOrder, Integer> {
    // Find all flight orders by order
    List<FlightOrder> findByOrder(Order o);

    // Dashboard: Show 2 most recent flight orders
    List<FlightOrder> findTop3ByOrderByIdDesc();

    // Full list view: All flight orders sorted by newest first
    List<FlightOrder> findAllByOrderByIdDesc();
}
