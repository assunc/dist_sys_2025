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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public List<Flight> searchFlights(String source, String destination, String dateStr) throws IOException, InterruptedException, URISyntaxException {
        String encodedSource = java.net.URLEncoder.encode(source, java.nio.charset.StandardCharsets.UTF_8);
        String encodedDestination = java.net.URLEncoder.encode(destination, java.nio.charset.StandardCharsets.UTF_8);
        String encodedDate = java.net.URLEncoder.encode(dateStr, java.nio.charset.StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(BASE_URL + "/flights/searchByDateAndRoute?source=" + encodedSource + "&destination=" + encodedDestination + "&departureDate=" + encodedDate))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readValue(response.body(), new TypeReference<>() {});
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
                            reservation.addFlightReservation(new FlightReservation(
                                    new Flight(allRequestParams.get("flight")),
                                    seat.get().getSeatNumber(),
                                    seat.get().getSeatId()
                            ));
                        }
                    }
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
