package com.example.springsoap;

import com.example.springsoap.Entities.FlightOrder;
import com.example.springsoap.Entities.Order;
import com.example.springsoap.Model.Flight;
import com.example.springsoap.Model.FlightReservation;
import com.example.springsoap.Model.Reservation;
import com.example.springsoap.Model.Seat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FlightService {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String BASE_URL = "http://dsg.centralindia.cloudapp.azure.com:8081";

    public List<FlightOrder> bookSeats(List<Long> seatIds, Order order) throws IOException, InterruptedException, URISyntaxException {
        List<FlightOrder> flightOrders = new ArrayList<>();

        for (Long seatId : seatIds) {
            HttpRequest reserveRequest = HttpRequest.newBuilder()
                    .uri(new URI(BASE_URL + "/seats/" + seatId + "/reserve"))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();
            client.send(reserveRequest, HttpResponse.BodyHandlers.ofString());

            HttpRequest bookingRequest = HttpRequest.newBuilder()
                    .uri(new URI(BASE_URL + "/bookings?seatId=" + seatId + "&status=booked"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> bookingResponse = client.send(bookingRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode bookingJson = mapper.readTree(bookingResponse.body());
            long bookingId = bookingJson.get("id").asLong();

            HttpRequest seatRequest = HttpRequest.newBuilder()
                    .uri(new URI(BASE_URL + "/seats/" + seatId))
                    .GET()
                    .build();
            HttpResponse<String> seatResponse = client.send(seatRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode seatJson = mapper.readTree(seatResponse.body());

            String seatNumber = seatJson.get("seatNumber").asText();
            long flightId = seatJson.get("flightNumber").asLong();

            HttpRequest flightRequest = HttpRequest.newBuilder()
                    .uri(new URI(BASE_URL + "/flights/" + flightId))
                    .GET()
                    .build();
            HttpResponse<String> flightResponse = client.send(flightRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode flightJson = mapper.readTree(flightResponse.body());

            String source = flightJson.get("source").asText();
            String destination = flightJson.get("destination").asText();
            String flightCode = flightJson.get("flightNumber").asText();

            String formattedFlightNumber = source + "-" + destination + "-" + flightCode;

            FlightOrder flightOrder = new FlightOrder();
            flightOrder.setOrder(order);
            flightOrder.setSeatNumber(seatNumber);
            flightOrder.setFlightNumber(formattedFlightNumber);
            flightOrder.setBookingId(bookingId);
            flightOrder.setStatus("booked");
            flightOrder.setAirlineSupplier(null);

            flightOrders.add(flightOrder);
        }

        return flightOrders;
    }
    public boolean reserveSeats(List<Long> seatIds, Order order, List<FlightOrder> flightOrders) throws IOException, InterruptedException, URISyntaxException {
        boolean allPending = true;

        for (Long seatId : seatIds) {
            System.out.println("Seat IDs for reserving "+seatIds);
            // Step 1: Reserve the seat
            HttpRequest reserveRequest = HttpRequest.newBuilder()
                    .uri(new URI(BASE_URL + "/seats/" + seatId + "/reserve"))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> reserveResponse= client.send(reserveRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("Reserve Response: "+reserveResponse.body());


            // Step 2: Create booking with status=pending
            System.out.println("SeatID before the booking: "+seatId);
            HttpRequest bookingRequest = HttpRequest.newBuilder()
                    .uri(new URI(BASE_URL + "/bookings?seatId=" + seatId + "&status=pending"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> bookingResponse = client.send(bookingRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("Booking Response: "+bookingResponse.body());
            JsonNode bookingJson = mapper.readTree(bookingResponse.body());
            long bookingId = bookingJson.get("id").asLong();
            System.out.println("Booking ID: "+bookingId);

            // Step 3: Get seat and flight info
            HttpRequest seatRequest = HttpRequest.newBuilder()
                    .uri(new URI(BASE_URL + "/seats/" + seatId))
                    .GET()
                    .build();
            HttpResponse<String> seatResponse = client.send(seatRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode seatJson = mapper.readTree(seatResponse.body());
            String seatNumber = seatJson.get("seatNumber").asText();
            long flightId = seatJson.get("flightNumber").asLong();
            System.out.println("Flight Number: " + flightId);

            HttpRequest flightRequest = HttpRequest.newBuilder()
                    .uri(new URI(BASE_URL + "/flights/" + flightId))
                    .GET()
                    .build();
            HttpResponse<String> flightResponse = client.send(flightRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode flightJson = mapper.readTree(flightResponse.body());
            String source = flightJson.get("source").asText();
            String destination = flightJson.get("destination").asText();
            String flightCode = flightJson.get("flightNumber").asText();

            String formattedFlightNumber = source + "-" + destination + "-" + flightCode;

            // Step 4: Create and collect FlightOrder
            FlightOrder flightOrder = new FlightOrder();
            flightOrder.setOrder(order);
            flightOrder.setSeatNumber(seatNumber);
            flightOrder.setFlightNumber(formattedFlightNumber);
            flightOrder.setBookingId(bookingId);
            flightOrder.setStatus("pending");
            flightOrder.setAirlineSupplier(null);

            flightOrders.add(flightOrder);
        }

        // Ensure all are pending
        for (FlightOrder fo : flightOrders) {
            if (!"pending".equalsIgnoreCase(fo.getStatus())) {
                allPending = false;
                break;
            }
        }
        System.out.println("All Pending orders: "+allPending);

        return allPending;
    }


    public boolean confirmSeats(Order order, List<FlightOrder> flightOrders) throws IOException, InterruptedException, URISyntaxException {
        boolean allBooked = true;

        for (FlightOrder flightOrder : flightOrders) {
            long bookingId = flightOrder.getBookingId();
            System.out.println("Flight Order in confirm seats: "+ bookingId);
            HttpRequest confirmRequest = HttpRequest.newBuilder()
                    .uri(new URI(BASE_URL + "/bookings/" + bookingId + "/confirm"))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> confirmResponse = client.send(confirmRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("Confirm response.............. "+confirmResponse.body());
            if (confirmResponse.statusCode() == 200) {
                flightOrder.setStatus("booked");
            } else {
                allBooked = false;
            }
        }

        if (allBooked) {
            order.setStatus("booked");
        }

        return allBooked;
    }



    public List<Flight> getAllFlights() throws IOException, InterruptedException, URISyntaxException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(BASE_URL + "/flights"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        List<Flight> flights = mapper.readValue(response.body(), new TypeReference<>() {});

        for (Flight flight : flights) {
            Duration duration = Duration.between(
                    flight.getDepartureTime().toLocalDateTime(),
                    flight.getArrivalTime().toLocalDateTime()
            );
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;
            flight.setDuration(String.format("%dh %02dm", hours, minutes));
        }
        return flights;
    }

    public List<Flight> searchFlights(String source, String destination, String date)
            throws IOException, InterruptedException, URISyntaxException {

        // Always fetch all flights (bypass strict supplier filters)
        String uri = BASE_URL + "/flights";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(uri))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        List<Flight> allFlights = mapper.readValue(response.body(), new TypeReference<>() {});

        // Normalize search terms
        String srcFilter = source != null ? source.toLowerCase() : "";
        String destFilter = destination != null ? destination.toLowerCase() : "";
        String dateFilter = date != null ? date.trim() : "";

        // Apply local filtering
        return allFlights.stream()
                .filter(f -> srcFilter.isEmpty() || f.getSource().toLowerCase().contains(srcFilter))
                .filter(f -> destFilter.isEmpty() || f.getDestination().toLowerCase().contains(destFilter))
                .filter(f -> {
                    if (dateFilter.isEmpty()) return true;
                    String flightDate = f.getDepartureTime().toLocalDateTime().toLocalDate().toString();
                    return flightDate.equals(dateFilter);
                })
                .collect(Collectors.toList());
    }


    public Flight getFlightById(Long id) throws IOException, InterruptedException, URISyntaxException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(BASE_URL + "/flights/" + id))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readValue(response.body(), Flight.class);
    }

    public List<Seat> getAvailableSeats(Long flightNumber, Seat.SeatClass classType) throws IOException, InterruptedException, URISyntaxException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(BASE_URL + "/seats/available?flightNumber=" + flightNumber + "&seatClass=" + classType))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readValue(response.body(), new TypeReference<>() {});
    }


    public boolean cancelBooking(long bookingId) throws IOException, InterruptedException, URISyntaxException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(BASE_URL + "/bookings/cancel?bookingId=" + bookingId))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200;
    }
    public boolean cancelBookings(List<FlightOrder> flightOrders) throws IOException, InterruptedException, URISyntaxException {
        boolean allCancelled = true;

        for (FlightOrder flightOrder : flightOrders) {
            long bookingId = flightOrder.getBookingId();
            HttpRequest cancelRequest = HttpRequest.newBuilder()
                    .uri(new URI(BASE_URL + "/bookings/" + bookingId + "/cancel"))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> cancelResponse = client.send(cancelRequest, HttpResponse.BodyHandlers.ofString());

            if (cancelResponse.statusCode() == 200) {
                flightOrder.setStatus("cancelled");
            } else {
                allCancelled = false;
            }
        }

        return allCancelled;
    }



    public void addFlightReservations(Map<String, String> allRequestParams, Reservation reservation) {
        if (allRequestParams.containsKey("allSeatsJson")) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                List<Seat> allSeats = mapper.readValue(allRequestParams.get("allSeatsJson"), new TypeReference<List<Seat>>() {});
                for (Map.Entry<String, String> entry : allRequestParams.entrySet()) {
                    if (entry.getKey().startsWith("seat") && entry.getValue().equals("on")) {
                        int seatId = Integer.parseInt(entry.getKey().split("-")[1]);
                        Optional<Seat> seat = allSeats.stream().filter((s) -> s.getSeatId() == seatId).findFirst();
                        if (seat.isPresent()) {
                            Flight flight = new Flight(allRequestParams.get("flight"));
                            if (reservation.getFlightReservations().stream()
                                    .filter((flightRes) -> Objects.equals(flightRes.getFlight().getFlightNumber(), flight.getFlightNumber()))
                                    .filter((flightRes) -> Objects.equals(flightRes.getSeatId(), seat.get().getSeatId()))
                                    .toList().isEmpty()) {
                                reservation.addFlightReservation(new FlightReservation(
                                        flight,
                                        seat.get().getSeatNumber(),
                                        seat.get().getSeatId()
                                ));
                            }
                        }
                    }
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
