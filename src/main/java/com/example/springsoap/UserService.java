package com.example.springsoap;


import com.example.springsoap.Entities.User;
import com.example.springsoap.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Transactional

    public User findOrCreateFromOidcUser(OidcUser oidcUser) {
        String auth0Id = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        String role = (String) oidcUser.getClaims().get("https://travelbroker.com/role");

        // System.out.println("🔍 Auth0 ID: " + auth0Id);
        // System.out.println(" Email: " + email);
        //System.out.println("Name: " + name);
        // Skip storing managers
        if (!"user".equals(role)) {
            System.out.println(" Skipping storage for role: " + role);
            return null;
        }
        Optional<User> existingUser = userRepository.findByAuth0Id(auth0Id);

        if (existingUser.isPresent()) {

            // System.out.println("User already exists in DB.");

            User user = existingUser.get();
            boolean changed = false;
            if (!email.equals(user.getEmail())) {
                user.setEmail(email);
                changed = true;
            }
            if (!name.equals(user.getName())) {
                user.setName(name);
                changed = true;
            }
            return changed ? userRepository.save(user) : user;
            //return existingUser.get();
        } else {
            User newUser = new User();
            newUser.setAuth0Id(auth0Id);
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setDeliveryAddress("");
            newUser.setPaymentInfo("");
            //System.out.println("New user inserted: " + saved.getAuth0Id());
            return userRepository.save(newUser);        }
    }
}