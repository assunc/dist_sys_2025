package com.example.springsoap.Repositories;

import com.example.springsoap.Entities.Broker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrokerRepository extends JpaRepository<Broker, Integer> {
    Broker findByUsername(String username);
}
