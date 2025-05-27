package com.supplier.controller;

import com.supplier.entity.FlightEntity;
import com.supplier.repository.FlightRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/flights")
public class FlightController {

    private final FlightRepository flightRepo;

    public FlightController(FlightRepository flightRepo) {
        this.flightRepo = flightRepo;
    }

    @GetMapping
    public List<FlightEntity> getAllFlights() {
        return flightRepo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightEntity> getFlightById(@PathVariable Long id) {
        return flightRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public FlightEntity addFlight(@RequestBody FlightEntity flight) {
        return flightRepo.save(flight);
    }

    @GetMapping("/search")
    public List<FlightEntity> searchFlights(@RequestParam String source, @RequestParam String destination) {
        return flightRepo.findBySourceAndDestination(source, destination);
    }
}
