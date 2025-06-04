package com.example.springsoap.Repository;

import com.example.springsoap.Entities.HotelOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelOrderRepository extends JpaRepository<HotelOrder, Integer> {
}
