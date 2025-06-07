package com.example.springsoap.Repository;

import com.example.springsoap.Entities.Order;
import com.example.springsoap.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByUser(User currentUser);
}