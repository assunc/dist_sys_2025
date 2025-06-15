package com.supplier.controller;

import com.supplier.entity.FlightEntity;
import com.supplier.repository.FlightRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;

@RestController
@RequestMapping("/flights")
public class FlightController {

    private final FlightRepository flightRepo;

    public FlightController(FlightRepository flightRepo) {
        this.flightRepo = flightRepo;
    }

    // GET /flights → list all flights
    @GetMapping
    public List<FlightEntity> getAllFlights() {
        return flightRepo.findAll();
    }

    // GET /flights/{id} → get flight by ID
    @GetMapping("/{id}")
    public ResponseEntity<FlightEntity> getFlightById(@PathVariable Long id) {
        return flightRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /flights → create new flight
    @PostMapping
    public FlightEntity addFlight(@RequestBody FlightEntity flight) {
        return flightRepo.save(flight);
    }

    // GET /flights/search?source=Brussels&destination=Paris
    @GetMapping("/search")
    public List<FlightEntity> searchFlights(@RequestParam String source, @RequestParam String destination) {
        return flightRepo.findBySourceAndDestination(source, destination);
    }
    @GetMapping("/searchByDate") // http://localhost:8081/flights/searchByDate?start=2025-06-10&end=2025-06-20
    public List<FlightEntity> searchFlightsByDate(
            @RequestParam("start") String startDateStr,
            @RequestParam("end") String endDateStr) {
        Timestamp start = Timestamp.valueOf(startDateStr + " 00:00:00");
        Timestamp end = Timestamp.valueOf(endDateStr + " 23:59:59");

        return flightRepo.findByDepartureTimeBetween(start, end);
    }
    @GetMapping("/searchByDateAndRoute")
    public List<FlightEntity> getFlightsBySourceDestinationAndDate(
            @RequestParam("source") String source,
            @RequestParam("destination") String destination,
            @RequestParam("departureDate") String departureDateStr) {

        // Parse the date strings to Timestamps: start of day and end of day
        Timestamp startOfDay = Timestamp.valueOf(departureDateStr + " 00:00:00");
        Timestamp endOfDay = Timestamp.valueOf(departureDateStr + " 23:59:59");

        // Use a custom query in the repository to find flights matching all criteria
        return flightRepo.findBySourceAndDestinationAndDepartureTimeBetween(
                source, destination, startOfDay, endOfDay);
    }

}
