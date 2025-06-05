package com.example.springsoap.Repository;

import com.example.springsoap.Entities.HotelOrder;
import com.example.springsoap.Entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface HotelOrderRepository extends JpaRepository<HotelOrder, Integer> {
    Collection<? extends HotelOrder> findByOrder(Order o);
}
