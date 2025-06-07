package com.example.springsoap.Repository;

import com.example.springsoap.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByAuth0Id(String auth0Id);
    Optional<User> findByEmail(String email);
}
