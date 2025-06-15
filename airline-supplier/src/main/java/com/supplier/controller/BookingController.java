package com.supplier.controller;

import com.supplier.entity.BookingEntity;
import com.supplier.entity.SeatEntity;
import com.supplier.repository.BookingRepository;
import com.supplier.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SeatRepository seatRepository;

    @PostMapping
    public BookingEntity createBooking(@RequestParam Long seatId,
                                       @RequestParam(defaultValue = "pending") String status) {
        BookingEntity booking = new BookingEntity(seatId, status);
        BookingEntity saved = bookingRepository.save(booking);
        updateSeatStatusByString(seatId, status);
        return saved;
    }

    @GetMapping
    public List<BookingEntity> getAllBookings() {
        return bookingRepository.findAll();
    }

    @GetMapping("/{id}")
    public BookingEntity getBookingById(@PathVariable Long id) {
        return bookingRepository.findById(id).orElse(null);
    }

    // 🔧 New method to update seat status
    private void updateSeatStatusByString(Long seatId, String status) {
        seatRepository.findById(seatId).ifPresent(seat -> {
            if ("BOOKED".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status)) {
                seat.setAvailable(false);
            } else if ("AVAILABLE".equalsIgnoreCase(status)) {
                seat.setAvailable(true);
            } else {
                throw new IllegalArgumentException("Invalid status: must be 'BOOKED' or 'AVAILABLE'");
            }
            seatRepository.save(seat);
        });
    }
    @DeleteMapping("/cancel")
    public ResponseEntity<String> cancelBooking(@RequestParam Long bookingId) {
        return bookingRepository.findById(bookingId).map(booking -> {
            Long seatId = booking.getSeatId(); // get seat ID before deleting

            bookingRepository.deleteById(bookingId); // delete booking row

            // set the seat as available
            seatRepository.findById(seatId).ifPresent(seat -> {
                seat.setAvailable(true);
                seatRepository.save(seat);
            });

            return ResponseEntity.ok("Booking deleted and seat released.");
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{bookingId}/confirm")
    public ResponseEntity<String> confirmBooking(@PathVariable Long bookingId) {
        Optional<BookingEntity> optionalBooking = bookingRepository.findById(bookingId);
        if (optionalBooking.isPresent()) {
            BookingEntity booking = optionalBooking.get();
            booking.setStatus("BOOKED");
            bookingRepository.save(booking);
            updateSeatStatusByString(booking.getSeatId(), "BOOKED");
            return ResponseEntity.ok("Booking confirmed.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Booking not found.");
        }
    }



}
