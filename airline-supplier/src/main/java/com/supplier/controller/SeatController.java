package com.supplier.controller;

import com.supplier.entity.SeatEntity;
import com.supplier.repository.SeatRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seats")
public class SeatController {

    private final SeatRepository seatRepo;

    public SeatController(SeatRepository seatRepo) {
        this.seatRepo = seatRepo;
    }

    // GET /seats → list all seats
    @GetMapping
    public List<SeatEntity> getAllSeats() {
        return seatRepo.findAll();
    }

    // GET /seats/{id} → get seat by ID
    @GetMapping("/{id}")
    public ResponseEntity<SeatEntity> getSeatById(@PathVariable Long id) {
        return seatRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /seats → create new seat
    @PostMapping
    public SeatEntity addSeat(@RequestBody SeatEntity seat) {
        return seatRepo.save(seat);
    }

    @GetMapping("/available")
    public List<SeatEntity> getAvailableSeatsByClass(
            @RequestParam Long flightNumber,
            @RequestParam SeatEntity.SeatClass seatClass) {
        return seatRepo.findByFlightNumberAndAvailableTrueAndSeatClass(flightNumber, seatClass);
    }

    // PUT /seats/{id}/reserve → reserve a seat (set available to false)
    @PutMapping("/{id}/reserve")
    public ResponseEntity<String> reserveSeat(@PathVariable Long id) {
        return seatRepo.findById(id).map(seat -> {
            if (seat.getAvailable()) {
                seat.setAvailable(false);
                seatRepo.save(seat);
                return ResponseEntity.ok("Seat reserved!");
            } else {
                return ResponseEntity.status(409).body("Seat already reserved!");
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    // PUT /seats/{id}/cancel → cancel a reservation
    @PutMapping("/{id}/cancel")
    public ResponseEntity<String> cancelSeat(@PathVariable Long id) {
        return seatRepo.findById(id).map(seat -> {
            seat.setAvailable(true);
            seatRepo.save(seat);
            return ResponseEntity.ok("Reservation canceled.");
        }).orElse(ResponseEntity.notFound().build());
    }
}
