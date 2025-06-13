package com.example.springsoap.Repository;

import com.example.springsoap.Entities.HotelOrder;
import com.example.springsoap.Entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface HotelOrderRepository extends JpaRepository<HotelOrder, Integer> {
    List<HotelOrder> findByOrder(Order o);

    // Get the 3 most recent hotel orders by start date (for dashboard preview)
    List<HotelOrder> findTop3ByOrderByStartDateDesc();

    // Get all hotel orders sorted by start date (for full list view)
    List<HotelOrder> findAllByOrderByStartDateDesc();

}
