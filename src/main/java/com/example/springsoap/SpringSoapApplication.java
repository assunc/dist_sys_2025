package com.example.springsoap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class SpringSoapApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringSoapApplication.class, args);

}

}