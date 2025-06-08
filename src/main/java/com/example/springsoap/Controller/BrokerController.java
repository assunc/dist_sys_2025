package com.example.springsoap.Controller;


import com.example.springsoap.Entities.FlightOrder;
import com.example.springsoap.Entities.Order;
import com.example.springsoap.Entities.User;
import com.example.springsoap.Model.Airline;

import com.example.springsoap.Entities.*;

import com.example.springsoap.Model.Hotel;
import com.example.springsoap.Model.Reservation;
import com.example.springsoap.Model.Room;

import com.example.springsoap.Model.Seat;

import com.example.springsoap.Model.RoomReservation;
import com.example.springsoap.Repository.AirlineSupplierRepository;
import com.example.springsoap.Repository.HotelOrderRepository;

import com.example.springsoap.Repository.FlightOrderRepository;
import com.example.springsoap.Repository.OrderRepository;
import com.example.springsoap.Repository.UserRepository;
import com.example.springsoap.UserService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.example.springsoap.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.client.RestTemplate;


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import java.util.*;


@Controller
public class BrokerController {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private FlightOrderRepository flightOrderRepository;
    @Autowired
    private UserRepository userRepository;

    private final UserService userService;
    private final HotelService hotelService;
    private final AirlineSupplierRepository airlineSupplierRepository;
    private final HotelOrderRepository hotelOrderRepository;
//    private final FlightOrderRepository flightOrderRepository;
//    private final OrderRepository orderRepository;
//    private final UserRepository userRepository;

    private final Reservation reservation;

    @Autowired
    public BrokerController(
            UserService userService, HotelService hotelService,
            Reservation reservation,
            AirlineSupplierRepository airlineSupplierRepository,
            HotelOrderRepository hotelOrderRepository,
            FlightOrderRepository flightOrderRepository,
            OrderRepository orderRepository, UserRepository userRepository
    ) {
        this.userService = userService;
        this.hotelService = hotelService;
        this.reservation = reservation;
        this.airlineSupplierRepository = airlineSupplierRepository;
        this.hotelOrderRepository = hotelOrderRepository;
        this.flightOrderRepository = flightOrderRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }


    @GetMapping("/")
    public String home(@AuthenticationPrincipal OidcUser user, Model model) {
        boolean isLoggedIn = user != null;
        if (isLoggedIn) {
            userService.findOrCreateFromOidcUser(user);

            String idToken = user.getIdToken().getTokenValue();
            System.out.println("\n\nID TOKEN:\n" + idToken + "\n");
            model.addAttribute("name", user.getFullName());
        }

        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Home");
        model.addAttribute("contentTemplate", "index");
        return "layout";
    }


    @GetMapping("/flights")
    public String flights(@AuthenticationPrincipal OidcUser user, Model model) throws URISyntaxException, IOException, InterruptedException {
        boolean isLoggedIn = user != null;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://dsg.centralindia.cloudapp.azure.com:8081/flights"))
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String json = response.body();

        ObjectMapper mapper = new ObjectMapper();

        List<Airline> flights = mapper.readValue(json, new TypeReference<List<Airline>>() {});

        for (Airline flight : flights) {
            Duration duration = Duration.between(
                    flight.getDepartureTime().toLocalDateTime(),
                    flight.getArrivalTime().toLocalDateTime()
            );
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;
            flight.setDuration(String.format("%dh %02dm", hours, minutes));
        }


        model.addAttribute("title", "Flights");
        model.addAttribute("flights", flights);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("contentTemplate", "flights");
        return "layout";
    }




    @PostMapping("/flights")
    public String searchFlights(@AuthenticationPrincipal OidcUser user,
                                @RequestParam("departure_time") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date departureDate,
                                @RequestParam("source") String source,
                                @RequestParam("destination") String destination,
                                Model model) throws IOException, InterruptedException, URISyntaxException {

        boolean isLoggedIn = user != null;

        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(departureDate);

        String encodedSource = URLEncoder.encode(source, StandardCharsets.UTF_8);
        String encodedDestination = URLEncoder.encode(destination, StandardCharsets.UTF_8);
        String encodedDate = URLEncoder.encode(dateStr, StandardCharsets.UTF_8);

        String uri = "http://dsg.centralindia.cloudapp.azure.com:8081/flights/searchByDateAndRoute?source=" + encodedSource +
                "&destination=" + encodedDestination + "&departureDate=" + encodedDate;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(uri))
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        List<Airline> flights = mapper.readValue(response.body(),
                new TypeReference<List<Airline>>() {});

        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("flights", flights);
        model.addAttribute("contentTemplate", "flights"); // 👈 Fix added!

        return "layout";
    }


//    @GetMapping("/flight-info/{id}")
//    public String flightDetails(@PathVariable Long id, Model model) throws IOException, InterruptedException, URISyntaxException {
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(new URI("http://localhost:8081/flights/" + id))
//                .GET()
//                .build();
//
//        HttpClient client = HttpClient.newHttpClient();
//        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//        ObjectMapper mapper = new ObjectMapper();
//        Airline flight = mapper.readValue(response.body(), Airline.class);
//        model.addAttribute("flight", flight);
//        return "flight-info";
//    }

    @GetMapping("/flight-info/{id}")
    public String flightDetails(@AuthenticationPrincipal OidcUser user, @PathVariable Long id, Model model) throws IOException, InterruptedException, URISyntaxException {
        boolean isLoggedIn = user != null;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://dsg.centralindia.cloudapp.azure.com:8081/flights/" + id))
                .GET()
                .build();


        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        Airline flight = mapper.readValue(response.body(), Airline.class);
        System.out.println(response.body());
        model.addAttribute("flight", flight);
        model.addAttribute("contentTemplate", "flight-info");
        model.addAttribute("isLoggedIn", isLoggedIn);
        return "layout";
    }


    @GetMapping("/book/{classType}/{flightNumber}")
    public String bookSeat(@AuthenticationPrincipal OidcUser user,
                           @PathVariable Seat.SeatClass classType,
                           @PathVariable Long flightNumber,
                           Model model) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://dsg.centralindia.cloudapp.azure.com:8081/seats/available?flightNumber=" + flightNumber + "&seatClass=" + classType))
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        List<Seat> seats = mapper.readValue(response.body(), new TypeReference<List<Seat>>() {});

        String seatsJson = mapper.writeValueAsString(seats);

        HttpRequest flightRequest = HttpRequest.newBuilder()
                .uri(new URI("http://dsg.centralindia.cloudapp.azure.com:8081/flights/" + flightNumber))
                .GET()
                .build();

        HttpResponse<String> flightResponse = client.send(flightRequest, HttpResponse.BodyHandlers.ofString());
        Airline flight = mapper.readValue(flightResponse.body(), Airline.class);


        model.addAttribute("classType", classType);
        model.addAttribute("flightNumber", flightNumber);
        model.addAttribute("seats", seats);
        model.addAttribute("seatsJson", seatsJson);
        model.addAttribute("flight", flight);
        model.addAttribute("isLoggedIn", user != null);
        model.addAttribute("contentTemplate", "flightBooking");


        return "layout";
    }


    @PostMapping("/payment-page-flight")
    public String showPaymentPage(
            @AuthenticationPrincipal OidcUser user,
            @RequestParam("allSeatsJson") String allSeatsJson,
            @RequestParam("selectedSeatIndices") List<Integer> selectedIndices,
            Model model) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        List<Seat> allSeats = mapper.readValue(allSeatsJson, new TypeReference<List<Seat>>() {});
        List<Seat> selectedSeats = selectedIndices.stream()
                .map(allSeats::get)
                .collect(Collectors.toList());

        model.addAttribute("selectedSeats", selectedSeats);
        model.addAttribute("isLoggedIn", user != null);
        model.addAttribute("contentTemplate", "payment-page-flight");
        return "layout";
    }

    @PostMapping("/final-payment")
    public String sendPayment(@AuthenticationPrincipal OidcUser user,
                              @RequestParam("selectedSeatIds") List<Integer> selectedSeatIds,
                              @RequestParam("cardNumber") String cardNumber,
                              @RequestParam("expirationMonth") int expirationMonth,
                              @RequestParam("expirationYear") int expirationYear,
                              @RequestParam("cvc") String cvc,
                              @RequestParam("billingStreet") String billingStreet,
                              @RequestParam("billingCity") String billingCity,
                              @RequestParam("billingPostalCode") String billingPostalCode,
                              @RequestParam("billingCountry") String billingCountry,
                              Model model) throws URISyntaxException, IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();
        List<String> responses = new ArrayList<>();
        List<FlightOrder> flightOrders = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        boolean isLoggedIn = user != null;

        // STEP 1: Create and save broker-side Order
        Order newOrder = new Order(
                isLoggedIn ? userService.findOrCreateFromOidcUser(user) : null,
                billingStreet + ", " + billingCity + ", " + billingPostalCode + ", " + billingCountry,
                cardNumber + ", " + expirationMonth + "/" + expirationYear + ", " + cvc,
                "booked"
        );
        orderRepository.save(newOrder);

        // STEP 2: Loop over each seat and create bookings + flight orders
        for (Integer seatId : selectedSeatIds) {
            // 2a. Reserve seat
            HttpRequest reserveRequest = HttpRequest.newBuilder()
                    .uri(new URI("http://dsg.centralindia.cloudapp.azure.com:8081/seats/" + seatId + "/reserve"))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> reserveResponse = client.send(reserveRequest, HttpResponse.BodyHandlers.ofString());

            // 2b. Create booking
            HttpRequest bookingRequest = HttpRequest.newBuilder()
                    .uri(new URI("http://dsg.centralindia.cloudapp.azure.com:8081/bookings?seatId=" + seatId + "&status=BOOKED"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> bookingResponse = client.send(bookingRequest, HttpResponse.BodyHandlers.ofString());

            // 2c. Extract bookingId
            JsonNode bookingJson = mapper.readTree(bookingResponse.body());
            long bookingId = bookingJson.get("id").asLong();

            // 2d. Get seat info from supplier
            HttpRequest seatRequest = HttpRequest.newBuilder()
                    .uri(new URI("http://dsg.centralindia.cloudapp.azure.com:8081/seats/" + seatId))
                    .GET()
                    .build();
            HttpResponse<String> seatResponse = client.send(seatRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode seatJson = mapper.readTree(seatResponse.body());

            String seatNumber = seatJson.get("seatNumber").asText();
            long flightId = seatJson.get("flightNumber").asLong(); // adjust if field is nested

            // 2e. Get flight info from supplier
            HttpRequest flightRequest = HttpRequest.newBuilder()
                    .uri(new URI("http://dsg.centralindia.cloudapp.azure.com:8081/flights/" + flightId))
                    .GET()
                    .build();
            HttpResponse<String> flightResponse = client.send(flightRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode flightJson = mapper.readTree(flightResponse.body());

            String source = flightJson.get("source").asText();
            String destination = flightJson.get("destination").asText();
            String flightCode = flightJson.get("flightNumber").asText(); // adjust field if needed

            String formattedFlightNumber = source + "-" + destination + "-" + flightCode;

            // 2f. Create FlightOrder
            FlightOrder flightOrder = new FlightOrder();
            flightOrder.setOrder(newOrder);
            flightOrder.setSeatNumber(seatNumber);
            flightOrder.setFlightNumber(formattedFlightNumber);
            flightOrder.setBookingId(bookingId);
            flightOrder.setStatus("booked");
            flightOrder.setAirlineSupplier(null); // set later if needed

            flightOrders.add(flightOrder);
            responses.add("Seat " + seatNumber + " on " + formattedFlightNumber + " reserved. Booking ID: " + bookingId);
        }

        // STEP 3: Save all flight orders
        flightOrderRepository.saveAll(flightOrders);

        // STEP 4: Prepare frontend
        model.addAttribute("reservationResponses", responses);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("contentTemplate", "final-payment");
        return "layout";
    }


    @PostMapping("/cancel-order")
    public String cancelFlightOrder(@RequestParam("flightOrderId") Integer flightOrderId,
                                    @AuthenticationPrincipal OidcUser user,@RequestParam("prevPage") String prevPage,
                                    Model model) throws IOException, InterruptedException, URISyntaxException {

        Optional<FlightOrder> optionalFlightOrder = flightOrderRepository.findById(flightOrderId);
        if (optionalFlightOrder.isEmpty()) {
            model.addAttribute("error", "Flight order not found.");
            return "error";
        }

        FlightOrder flightOrder = optionalFlightOrder.get();
        long bookingId = flightOrder.getBookingId();
        System.out.println("...............................");
        System.out.println(bookingId);
        System.out.println("...............................");
        // Call supplier API to cancel booking
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://dsg.centralindia.cloudapp.azure.com:8081/bookings/cancel?bookingId=" + bookingId))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {

            flightOrder.setStatus("canceled");
            flightOrderRepository.save(flightOrder);

            Order order = flightOrder.getOrder();
            order.setStatus("canceled");
            orderRepository.save(order);

            return "redirect:/" + prevPage; // Redirect back to the previous page
        } else {
            model.addAttribute("error", "Failed to cancel booking: " + response.body());
            return "error";
        }
    }













    @GetMapping("/hotels")
    public String hotels(@AuthenticationPrincipal OidcUser user, Model model) {
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Hotels");
        model.addAttribute("contentTemplate", "hotels");
        // Initialize rooms list to empty when first loading the page
        model.addAttribute("hotels", List.of());
        model.addAttribute("error", false);
        model.addAttribute("searchPerformed", false);
        model.addAttribute("hasHotels", false);
        return "layout";
    }

    @PostMapping("/hotels")
    public String searchHotels(
            @RequestParam("destination") String destination,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate,
            @RequestParam("numberOfPeople") int numberOfPeople,
            @AuthenticationPrincipal OidcUser user,
            Model model
    ) {
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Hotels");
        model.addAttribute("contentTemplate", "hotels");

        try {
            List<Hotel> freeHotels = hotelService.getFreeHotels(startDate, endDate, destination);

            model.addAttribute("hotels", freeHotels);
            model.addAttribute("searchPerformed", true);
            model.addAttribute("hasHotels", !freeHotels.isEmpty());
            model.addAttribute("error", false);

        } catch (Exception e) {
            System.err.println("Exception during hotel search: " + e.getMessage());
            e.printStackTrace(); // Print full stack trace for detailed debugging
            model.addAttribute("error", "Error searching for hotels: " + e.getMessage());
            model.addAttribute("hotels", List.of()); // Ensure rooms list is empty on error
            model.addAttribute("searchPerformed", true);
            model.addAttribute("hasHotels", false);
        }

        // Retain search parameters for display on the page
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("numberOfPeople", numberOfPeople);

        return "layout";
    }

    @PostMapping("/hotel-info/{hotelId}")
    public String hotelInfo(@PathVariable int hotelId,
                            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
                            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate,
                            @RequestParam("numberOfPeople") int numberOfPeople,
                            @AuthenticationPrincipal OidcUser user, Model model
    ) {
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Hotel Info");
        model.addAttribute("contentTemplate", "hotel-info");
        try {
            Hotel hotel = hotelService.getHotel(hotelId);
            model.addAttribute("hotel", hotel);
            model.addAttribute("error", false);
        } catch (Exception e) {
            System.err.println("Exception during hotel search: " + e.getMessage());
            e.printStackTrace(); // Print full stack trace for detailed debugging
            model.addAttribute("error", "Error searching for hotels: " + e.getMessage());
            model.addAttribute("hotel", null); // Ensure rooms list is empty on error
        }

        try {
            List<Room> filteredRooms = hotelService.getFreeRooms(hotelId, startDate, endDate, numberOfPeople);
            model.addAttribute("rooms", filteredRooms);
            model.addAttribute("searchPerformed", true);
            model.addAttribute("hasRooms", !filteredRooms.isEmpty());
            model.addAttribute("error", false);
        } catch (Exception e) {
            System.err.println("Exception during room search: " + e.getMessage());
            e.printStackTrace(); // Print full stack trace for detailed debugging
            model.addAttribute("error", "Error searching for rooms: " + e.getMessage());
            model.addAttribute("rooms", List.of()); // Ensure rooms list is empty on error
            model.addAttribute("searchPerformed", true);
            model.addAttribute("hasRooms", false);
        }

        // Retain search parameters for display on the page
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("numberOfPeople", numberOfPeople);

        return "layout";
    }

    @PostMapping("/add-to-cart")
    public String addToCart(@RequestParam Map<String, String> allRequestParams, @AuthenticationPrincipal OidcUser user, Model model) {
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Payment Information");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date startDate, endDate;
        try {
            startDate = dateFormat.parse(allRequestParams.get("startDate"));
            endDate = dateFormat.parse(allRequestParams.get("endDate"));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        // Iterate through all request parameters to find selected room checkboxes
        // And the value is "on" when checked.
        for (Map.Entry<String, String> entry : allRequestParams.entrySet()) {
            if (entry.getKey().startsWith("Room") && entry.getValue().equals("on")) {
                reservation.addRoomReservation(new RoomReservation(
                        new Room(entry.getKey()),
                        allRequestParams.get("name"),
                        startDate, endDate
                ));
            }
        }

        return "redirect:/shopping-cart";
    }

    @PostMapping("/payment")
    public String payment(@RequestParam Map<String, String> allRequestParams, @AuthenticationPrincipal OidcUser user, Model model) {
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Payment Information");
        model.addAttribute("contentTemplate", "payment");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date startDate, endDate;
        if (allRequestParams.containsKey("startDate") && allRequestParams.containsKey("endDate")) {
            try {
                startDate = dateFormat.parse(allRequestParams.get("startDate"));
                endDate = dateFormat.parse(allRequestParams.get("endDate"));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            // Iterate through all request parameters to find selected room checkboxes
            // And the value is "on" when checked.
            for (Map.Entry<String, String> entry : allRequestParams.entrySet()) {
                if (entry.getKey().startsWith("Room") && entry.getValue().equals("on")) {
                    reservation.addRoomReservation(new RoomReservation(
                            new Room(entry.getKey()),
                            allRequestParams.get("name"),
                            startDate, endDate
                    ));
                }
            }
        }

        model.addAttribute("reservation", reservation);

        // --- Autofill Logic ---
        String userAddress = null;
        String userCity = null;
        String userPostalCode = null;
        String userCountry = null;
        String userCardNumber = null;
        String userExpirationMonth = null;
        String userExpirationYear = null;
        String userCvc = null;
        if (isLoggedIn) {
            User userDb = userRepository.findByEmail(user.getEmail()).orElseThrow(() -> new RuntimeException("User not found in DB"));
            String deliveryAddress = userDb.getDeliveryAddress();
            String paymentInfo = userDb.getPaymentInfo();
            if (!paymentInfo.isEmpty()) {
                String[] paymentInfoSplit = paymentInfo.split("_");
                userCardNumber = paymentInfoSplit[0];
                userExpirationMonth = paymentInfoSplit[1];
                userExpirationYear = paymentInfoSplit[2];
                userCvc = paymentInfoSplit[3];
            }
            if (!deliveryAddress.isEmpty()) {
                String[] deliveryAddressSplit = deliveryAddress.split("_");
                userAddress = deliveryAddressSplit[0];
                userCity = deliveryAddressSplit[1];
                userPostalCode = deliveryAddressSplit[2];
                userCountry = deliveryAddressSplit[3];
            }
        }
        model.addAttribute("userAddress", userAddress);
        model.addAttribute("userCity", userCity);
        model.addAttribute("userPostalCode", userPostalCode);
        model.addAttribute("userCountry", userCountry);
        model.addAttribute("userCardNumber", userCardNumber);
        model.addAttribute("userExpirationMonth", userExpirationMonth);
        model.addAttribute("userExpirationYear", userExpirationYear);
        model.addAttribute("userCvc", userCvc);

        return "layout";
    }

    @PostMapping("/process-reservation")
    public String processReservation(
            @RequestParam("cardNumber") String cardNumber,
            @RequestParam("expirationMonth") int expirationMonth,
            @RequestParam("expirationYear") int expirationYear,
            @RequestParam("cvc") String cvc,
            @RequestParam("billingStreet") String billingStreet,
            @RequestParam("billingCity") String billingCity,
            @RequestParam("billingPostalCode") String billingPostalCode,
            @RequestParam("billingCountry") String billingCountry,
            @AuthenticationPrincipal OidcUser user,
            Model model
    ) {
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Process Reservation");
        model.addAttribute("contentTemplate", "confirmation");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        Order newOrder = new Order(
                isLoggedIn ? userService.findOrCreateFromOidcUser(user) : null,
                billingStreet + ", " + billingCity + ", " + billingPostalCode + ", " + billingCountry,
                cardNumber + ", " + expirationMonth + "/" + expirationYear + ", " + cvc,
                "pending"
        );

        List<HotelOrder> hotelOrders = new ArrayList<>();
        List<FlightOrder> flightOrders = new ArrayList<>();

        // Two stage commit
        boolean allBookingsPending = true;
        boolean allBookingsBooked = true;
        boolean allBookingsCanceled = false;

        if (!reservation.getRoomReservations().isEmpty()) {
            try {
                allBookingsPending = hotelService.reserveOrders(reservation.getRoomReservations(), newOrder, hotelOrders);
                model.addAttribute("error", false);
            } catch (Exception e) {
                System.err.println("Exception during booking search: " + e.getMessage());
                e.printStackTrace(); // Print full stack trace for detailed debugging
                model.addAttribute("error", "Error searching for hotels: " + e.getMessage());
            }
        }

        if (!reservation.getFlightReservations().isEmpty()) {
            // add stage 1 of commit here
        }

        // save stage 1 of commit
        orderRepository.save(newOrder);
        hotelOrderRepository.saveAll(hotelOrders);
        flightOrderRepository.saveAll(flightOrders);

        if (allBookingsPending) { // Stage 2
            if (!reservation.getRoomReservations().isEmpty()) {
                try {
                    allBookingsBooked = hotelService.confirmOrders(newOrder, hotelOrders);
                    model.addAttribute("error", false);
                } catch (Exception e) {
                    System.err.println("Exception during booking search: " + e.getMessage());
                    e.printStackTrace(); // Print full stack trace for detailed debugging
                    model.addAttribute("error", "Error searching for hotels: " + e.getMessage());
                }
            }
            if (!reservation.getFlightReservations().isEmpty()) {
                // add stage 2 of commit here
            }
        }

        if (!allBookingsPending || !allBookingsBooked) { // Abort
            if (!reservation.getRoomReservations().isEmpty()) {
                try {
                    allBookingsCanceled = hotelService.cancelOrders(newOrder, hotelOrders);
                    model.addAttribute("error", false);
                } catch (Exception e) {
                    System.err.println("Exception during booking search: " + e.getMessage());
                    e.printStackTrace(); // Print full stack trace for detailed debugging
                    model.addAttribute("error", "Error searching for hotels: " + e.getMessage());
                }
            }
            if (!reservation.getFlightReservations().isEmpty()) {
                // add abort of commit here
            }
        }

        if (allBookingsBooked) {
            // everything went well
            reservation.clear();
            // save stage 2 of commit
            orderRepository.save(newOrder);
            hotelOrderRepository.saveAll(hotelOrders);
            flightOrderRepository.saveAll(flightOrders);
        }

        if (allBookingsCanceled) {
            // transaction aborted
            // save abort of commit
            orderRepository.save(newOrder);
            hotelOrderRepository.saveAll(hotelOrders);
            flightOrderRepository.saveAll(flightOrders);
        }

        return "layout";
    }

    @GetMapping("/shopping-cart")
    public String viewShoppingCart(Model model, @AuthenticationPrincipal OidcUser user) {
        boolean isLoggedIn = (user != null);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "My Bookings");
        model.addAttribute("contentTemplate", "shopping-cart");
        model.addAttribute("reservation", reservation);

        return "layout";
    }

    @GetMapping("/bookings")
    public String viewBookings(Model model, @AuthenticationPrincipal OidcUser user) throws IOException, InterruptedException, URISyntaxException {
        boolean isLoggedIn = (user != null);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "My Bookings");
        model.addAttribute("contentTemplate", "bookings");

        if (isLoggedIn) {
            User currentUser = userService.findOrCreateFromOidcUser(user);
            List<Order> userOrders = orderRepository.findByUser(currentUser);

            // ✅ Hotel Orders
            List<HotelOrder> userHotelOrders = new ArrayList<>();
            for (Order order : userOrders) {
                userHotelOrders.addAll(hotelOrderRepository.findByOrder(order));
            }
            userHotelOrders.sort(Comparator.comparing(HotelOrder::getStartDate));
            model.addAttribute("hotelOrders", userHotelOrders);

            // ✅ Flight Orders
            List<FlightOrder> userFlightOrders = flightOrderRepository.findByOrderIn(userOrders);
            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper();

            for (FlightOrder flight : userFlightOrders) {
                String flightNum = flight.getFlightNumber();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(new URI("http://dsg.centralindia.cloudapp.azure.com:8081/flights/flightNumber/" + flightNum))
                        .GET()
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    JsonNode flightJson = mapper.readTree(resp.body());
                    String departureTime = flightJson.get("departureTime").asText();
                    flight.setStatus(flight.getStatus() + " @ " + departureTime);
                }
            }

            model.addAttribute("flightOrders", userFlightOrders);
        } else {
            model.addAttribute("hotelOrders", List.of());
            model.addAttribute("flightOrders", List.of());
        }

        return "layout";
    }


    @PostMapping("/bookings/cancelHotel")
    public String cancelHotelBooking(@RequestParam("hotelOrderId") Integer hotelOrderId,
                                     @RequestParam("prevPage") String prevPage,
                                     Model model, @AuthenticationPrincipal OidcUser user) {
        boolean isLoggedIn = (user != null);
        model.addAttribute("isLoggedIn", isLoggedIn);
        Optional<HotelOrder> hotelOrder = hotelOrderRepository.findById(hotelOrderId);
        if (hotelOrder.isPresent() && !hotelOrder.get().getStatus().equalsIgnoreCase("cancelled")) {
            try {
                hotelService.cancelOrders(hotelOrder.get().getOrder(), List.of(hotelOrder.get()));
                hotelOrderRepository.save(hotelOrder.get());
                model.addAttribute("error", false);
            } catch (Exception e) {
                System.err.println("Exception during booking search: " + e.getMessage());
                e.printStackTrace(); // Print full stack trace for detailed debugging
                model.addAttribute("error", "Error searching for hotels: " + e.getMessage());
            }
        }

        return "redirect:/" + prevPage; // Redirect back to the previous page
    }

    @GetMapping("/combo")
    public String combo(@AuthenticationPrincipal OidcUser user, Model model) {
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Hotels + Flights");
        model.addAttribute("contentTemplate", "combo");
        return "layout";
    }
    @GetMapping("/manager/dashboard")
    public String managerDashboard(@AuthenticationPrincipal OidcUser user, Model model) {
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Dashboard");
        model.addAttribute("contentTemplate", "dashboard");

        List<HotelOrder> latestHotelOrders = hotelOrderRepository.findTop3ByOrderByStartDateDesc();
        List<FlightOrder> latestFlightOrders = flightOrderRepository.findTop3ByOrderByIdDesc();
        //List<Order> latestComboOrders = orderRepository.findTop2ByOrderByCreatedAtDesc(); // Combo logic

        model.addAttribute("hotelOrders", latestHotelOrders);
        model.addAttribute("flightOrders", latestFlightOrders);
        //model.addAttribute("comboOrders", latestComboOrders);

        return "layout";
    }
    @GetMapping("/manager/orders/{type}")
    public String viewAllOrders(
            @PathVariable String type,
            @AuthenticationPrincipal OidcUser user,
            @RequestParam(value = "status", required = false) String status,
            Model model
    ) {
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);

        model.addAttribute("title", "All " + type + " Orders");
        if (status == null || status.isBlank() || status.equalsIgnoreCase("all")) {
            status = null;
        }
        String finalStatus = status;

        switch (type.toLowerCase()) {
            case "hotel":
                List<HotelOrder> allHotelOrders = hotelOrderRepository.findAll();
                if (finalStatus != null) {
                    allHotelOrders = allHotelOrders.stream()
                            .filter(o -> o.getStatus().equalsIgnoreCase(finalStatus))
                            .toList();
                }

                model.addAttribute("status", finalStatus); // keep track of selected filter
                model.addAttribute("hotelOrders", allHotelOrders);
                model.addAttribute("contentTemplate", "manager-orders-hotel");
                break;
            case "flight":
                List<FlightOrder> allFlightOrders = flightOrderRepository.findAll();
                if (finalStatus != null) {
                    allFlightOrders = allFlightOrders.stream()
                            .filter(o -> o.getStatus().equalsIgnoreCase(finalStatus))
                            .toList();
                }

                model.addAttribute("status", finalStatus); // keep track of selected filter
                model.addAttribute("flightOrders", allFlightOrders); // use filtered result
                model.addAttribute("contentTemplate", "manager-orders-flight");
                break;
//            case "combo":
//                model.addAttribute("orders", orderRepository.findAll()); // Optional: filter combo-tagged ones
//                model.addAttribute("contentTemplate", "manager-orders-combo");
//                break;
            default:
                return "redirect:/manager/dashboard";
        }

        return "layout";
    }


    @GetMapping("/logged-out")
    public String loggedOut(@AuthenticationPrincipal OidcUser user, Model model) {
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Logged Out");
        model.addAttribute("contentTemplate", "loggedout");
        return "layout";
    }
}