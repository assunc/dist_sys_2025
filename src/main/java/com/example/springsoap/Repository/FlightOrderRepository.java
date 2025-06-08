package com.example.springsoap.Repository;

import com.example.springsoap.Entities.FlightOrder;
import com.example.springsoap.Entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlightOrderRepository extends JpaRepository<FlightOrder, Integer> {

    List<FlightOrder> findByOrder(Order o);

    List<FlightOrder> findByOrderIn(List<Order> orders); // 🔹 Add this

    List<FlightOrder> findTop3ByOrderByIdDesc();


    List<FlightOrder> findAllByOrderByIdDesc();
}
