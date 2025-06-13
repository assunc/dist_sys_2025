package com.example.springsoap.Controller;


import com.example.springsoap.Entities.FlightOrder;
import com.example.springsoap.Entities.Order;
import com.example.springsoap.Entities.User;
import com.example.springsoap.FlightService;
import com.example.springsoap.Model.*;

import com.example.springsoap.Entities.*;

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

import com.example.springsoap.HotelService;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


@Controller
public class BrokerController {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private FlightOrderRepository flightOrderRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FlightService flightService;


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

        List<Flight> flights = flightService.getAllFlights();

        model.addAttribute("title", "Flights");
        model.addAttribute("flights", flights);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("contentTemplate", "flights");
        return "layout";
    }

    @PostMapping("/flights")
    public String searchFlights(@AuthenticationPrincipal OidcUser user,
                                @RequestParam("departure_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date departureDate,
                                @RequestParam("source") String source,
                                @RequestParam("destination") String destination,
                                Model model) throws IOException, InterruptedException, URISyntaxException {

        boolean isLoggedIn = user != null;

        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(departureDate);

        String encodedSource = URLEncoder.encode(source, StandardCharsets.UTF_8);
        String encodedDestination = URLEncoder.encode(destination, StandardCharsets.UTF_8);
        String encodedDate = URLEncoder.encode(dateStr, StandardCharsets.UTF_8);
        List<Flight> flights = flightService.searchFlights(encodedSource, encodedDestination, encodedDate);

        model.addAttribute("departure_date", departureDate);
        model.addAttribute("source", source);
        model.addAttribute("destination", destination);

        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("flights", flights);
        model.addAttribute("contentTemplate", "flights");

        return "layout";
    }

    @GetMapping("/flight-info/{id}")
    public String flightDetails(@AuthenticationPrincipal OidcUser user, @PathVariable Long id, Model model) throws IOException, InterruptedException, URISyntaxException {
        boolean isLoggedIn = user != null;

        Flight flight = flightService.getFlightById(id);
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

        // Fetch available seats
        List<Seat> seats = flightService.getAvailableSeats(flightNumber, classType);

        ObjectMapper mapper = new ObjectMapper();

        // Serialize seats to JSON
        String seatsJson = mapper.writeValueAsString(seats);

        // Fetch flight details
        Flight flight = flightService.getFlightById(flightNumber);

        model.addAttribute("classType", classType);
        model.addAttribute("flightNumber", flightNumber);
        model.addAttribute("seats", seats);
        model.addAttribute("seatsJson", seatsJson);
        model.addAttribute("flight", flight);
        model.addAttribute("isLoggedIn", user != null);
        model.addAttribute("contentTemplate", "flightBooking");

        return "layout";
    }

    @PostMapping("/cancel-order")
    public String cancelFlightOrder(@RequestParam("flightOrderId") Integer flightOrderId,
                                    @AuthenticationPrincipal OidcUser user,
                                    @RequestParam("prevPage") String prevPage,
                                    Model model) throws IOException, InterruptedException, URISyntaxException {

        Optional<FlightOrder> optionalFlightOrder = flightOrderRepository.findById(flightOrderId);
        if (optionalFlightOrder.isEmpty()) {
            model.addAttribute("error", "Flight order not found.");
            return "error";
        }

        FlightOrder flightOrder = optionalFlightOrder.get();
        long bookingId = flightOrder.getBookingId();

        boolean cancelSuccess = flightService.cancelBooking(bookingId);

        if (cancelSuccess) {
            flightOrder.setStatus("canceled");
            flightOrderRepository.save(flightOrder);

            Order order = flightOrder.getOrder();

            boolean allFlightsCanceled = flightOrderRepository.findByOrder(order).stream()
                    .allMatch(f -> f.getStatus().equalsIgnoreCase("canceled"));
            boolean allHotelsCanceled = hotelOrderRepository.findByOrder(order).stream()
                    .allMatch(h -> h.getStatus().equalsIgnoreCase("canceled"));
            if (allFlightsCanceled && allHotelsCanceled) {
                order.setStatus("canceled");
                orderRepository.save(order);
            }

            return "redirect:/" + prevPage;
        } else {
            model.addAttribute("error", "Failed to cancel booking via supplier.");
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

        hotelService.addHotelReservations(allRequestParams, reservation);
        flightService.addFlightReservations(allRequestParams, reservation);

        return "redirect:/shopping-cart";
    }

    @PostMapping("/payment")
    public String payment(@RequestParam Map<String, String> allRequestParams, @AuthenticationPrincipal OidcUser user, Model model) {
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Payment Information");
        model.addAttribute("contentTemplate", "payment");

        hotelService.addHotelReservations(allRequestParams, reservation);
        flightService.addFlightReservations(allRequestParams, reservation);

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
    ) throws URISyntaxException, IOException, InterruptedException {
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Process Reservation");
        model.addAttribute("contentTemplate", "final-payment"); //Confirmation

        Order newOrder = new Order(
                isLoggedIn ? userService.findOrCreateFromOidcUser(user) : null,
                billingStreet + ", " + billingCity + ", " + billingPostalCode + ", " + billingCountry,
                cardNumber + ", " + expirationMonth + "/" + expirationYear + ", " + cvc,
                "booked"
        );

        List<HotelOrder> hotelOrders = new ArrayList<>();
        List<FlightOrder> flightOrders = new ArrayList<>();

        // --- Two-phase commit flags ---
        boolean allBookingsPending = true;
        boolean allBookingsBooked = true;
        boolean allBookingsCanceled = false;

        // --- Phase 1: Reserve ---
        // Hotel
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
            List<Long> seatIds = reservation.getFlightReservations().stream().map(FlightReservation::getSeatId).toList();
            try {
                allBookingsPending &= flightService.reserveSeats(seatIds, newOrder, flightOrders);
                model.addAttribute("error", false);
            } catch (Exception e) {
                System.err.println("Flight reservation error: " + e.getMessage());
                e.printStackTrace();
                model.addAttribute("error", "Error during flight reservation.");
            }
        }

        // Save phase 1 orders
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

            // Flight
            if (!flightOrders.isEmpty()) {
                try {
                    allBookingsBooked &= flightService.confirmSeats(newOrder, flightOrders);
                    model.addAttribute("error", false);
                } catch (Exception e) {
                    System.err.println("Flight confirmation error: " + e.getMessage());
                    e.printStackTrace();
                    model.addAttribute("error", "Error confirming flight booking.");
                    allBookingsBooked = false;
                }
            }
        }

        // --- Finalize Commit ---
        if (allBookingsBooked) {
            reservation.clear();
            orderRepository.save(newOrder);
            hotelOrderRepository.saveAll(hotelOrders);
            flightOrderRepository.saveAll(flightOrders);
        }

        // --- Abort if needed ---
        if (!allBookingsPending || !allBookingsBooked) {
            // Hotel cancel
            if (!reservation.getRoomReservations().isEmpty()) {
                try {
                    allBookingsCanceled = hotelService.cancelOrders(newOrder, hotelOrders, true);
                    model.addAttribute("error", false);
                } catch (Exception e) {
                    System.err.println("Hotel cancellation error: " + e.getMessage());
                    e.printStackTrace();
                    model.addAttribute("error", "Error cancelling hotel orders.");
                }
            }

            // Flight cancel
            if (!flightOrders.isEmpty()) {
                try {
                    allBookingsCanceled &= flightService.cancelBookings(flightOrders);
                    model.addAttribute("error", false);
                } catch (Exception e) {
                    System.err.println("Flight cancellation error: " + e.getMessage());
                    e.printStackTrace();
                    model.addAttribute("error", "Error cancelling flight orders.");
                }
            }

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

            // Hotel Orders
            List<HotelOrder> userHotelOrders = new ArrayList<>();
            for (Order order : userOrders) {
                userHotelOrders.addAll(hotelOrderRepository.findByOrder(order));
            }
            userHotelOrders.sort(Comparator.comparing(HotelOrder::getStartDate));
            model.addAttribute("hotelOrders", userHotelOrders);

            // Flight Orders
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

        Optional<HotelOrder> hotelOrderOpt = hotelOrderRepository.findById(hotelOrderId);
        if (hotelOrderOpt.isPresent() && !hotelOrderOpt.get().getStatus().equalsIgnoreCase("canceled")) {
            HotelOrder hotelOrder = hotelOrderOpt.get();
            Order order = hotelOrder.getOrder();

            try {
                // Cancel only this booking
                hotelService.cancelOrders(order, List.of(hotelOrder), false);

                // Save the updated hotel order
                hotelOrderRepository.save(hotelOrder);

                // ✅ Check if ALL hotel and flight bookings for this order are canceled
                boolean allHotelsCanceled = hotelOrderRepository.findByOrder(order).stream()
                        .allMatch(h -> h.getStatus().equalsIgnoreCase("canceled"));
                boolean allFlightsCanceled = flightOrderRepository.findByOrder(order).stream()
                        .allMatch(f -> f.getStatus().equalsIgnoreCase("canceled"));

                // Only now set the order as canceled
                if (allHotelsCanceled && allFlightsCanceled) {
                    order.setStatus("canceled");
                    orderRepository.save(order);
                }

                model.addAttribute("error", false);

            } catch (Exception e) {
                System.err.println("Exception during hotel cancellation: " + e.getMessage());
                e.printStackTrace();
                model.addAttribute("error", "Error cancelling hotel order: " + e.getMessage());
            }
        }

        return "redirect:/" + prevPage;
    }

    @GetMapping("/combo")
    public String combo(@AuthenticationPrincipal OidcUser user, Model model) {
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Hotels + Flights");
        model.addAttribute("contentTemplate", "combo");
        model.addAttribute("searchPerformed", false);  // initial page
        return "layout";
    }



    @GetMapping("/combo/search")
    public String searchCombo(
            @RequestParam("source") String source,
            @RequestParam("destination") String destination,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate,
            @RequestParam("numberOfPeople") int numberOfPeople,
            @AuthenticationPrincipal OidcUser user,
            Model model
    ) {
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Hotels + Flights");

        try {
            List<Hotel> hotels = hotelService.getFreeHotels(startDate, endDate, destination);
            List<Flight> outboundFlights = flightService.searchFlights(source, destination, new SimpleDateFormat("yyyy-MM-dd").format(startDate));
            List<Flight> returnFlights = flightService.searchFlights(destination, source, new SimpleDateFormat("yyyy-MM-dd").format(endDate));

            model.addAttribute("hotels", hotels);
            model.addAttribute("outboundFlights", outboundFlights);
            model.addAttribute("returnFlights", returnFlights);
            model.addAttribute("searchPerformed", true);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to search combo package: " + e.getMessage());
        }

        // Retain the search inputs in the form
        model.addAttribute("source", source);
        model.addAttribute("destination", destination);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("numberOfPeople", numberOfPeople);
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