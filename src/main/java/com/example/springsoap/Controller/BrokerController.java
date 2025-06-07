package com.example.springsoap.Controller;

import com.example.springsoap.Entities.*;
import com.example.springsoap.Model.Hotel;
import com.example.springsoap.Model.Reservation;
import com.example.springsoap.Model.Room;
import com.example.springsoap.Model.RoomReservation;
import com.example.springsoap.Repository.AirlineSupplierRepository;
import com.example.springsoap.Repository.HotelOrderRepository;
import com.example.springsoap.Repository.FlightOrderRepository;
import com.example.springsoap.Repository.OrderRepository;
import com.example.springsoap.UserService;
import com.example.springsoap.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class BrokerController {
    private final UserService userService;
    private final HotelService hotelService;
    private final AirlineSupplierRepository airlineSupplierRepository;
    private final HotelOrderRepository hotelOrderRepository;
    private final FlightOrderRepository flightOrderRepository;
    private final OrderRepository orderRepository;

    private final Reservation reservation;

    @Autowired
    public BrokerController(
            UserService userService, HotelService hotelService,
            Reservation reservation,
            AirlineSupplierRepository airlineSupplierRepository,
            HotelOrderRepository hotelOrderRepository,
            FlightOrderRepository flightOrderRepository,
            OrderRepository orderRepository
    ) {
        this.userService = userService;
        this.hotelService = hotelService;
        this.reservation = reservation;
        this.airlineSupplierRepository = airlineSupplierRepository;
        this.hotelOrderRepository = hotelOrderRepository;
        this.flightOrderRepository = flightOrderRepository;
        this.orderRepository = orderRepository;
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
    public String flights(@AuthenticationPrincipal OidcUser user, Model model) {
        boolean isLoggedIn = user != null;
        model.addAttribute("title", "Flights");
        model.addAttribute("flights", List.of("BRU → NYC", "BRU → TOKYO"));
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("contentTemplate", "flights");
        return "layout";
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
        if (isLoggedIn) {
            userAddress = "Rue du Marché aux Herbes 10";
            userCity = "Leuven";
            userPostalCode = "3000";
            userCountry = "Belgium";
        }
        model.addAttribute("userAddress", userAddress);
        model.addAttribute("userCity", userCity);
        model.addAttribute("userPostalCode", userPostalCode);
        model.addAttribute("userCountry", userCountry);

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
    public String viewBookings(Model model, @AuthenticationPrincipal OidcUser user) {
        boolean isLoggedIn = (user != null);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "My Bookings");
        model.addAttribute("contentTemplate", "bookings");

        if (isLoggedIn) {
            User currentUser = userService.findOrCreateFromOidcUser(user);

            // Fetch hotel orders associated with this user's orders
            List<HotelOrder> userHotelOrders = new ArrayList<>();
            for (Order order : orderRepository.findByUser(currentUser)) {
                userHotelOrders.addAll(hotelOrderRepository.findByOrder(order));
            }

            // Sort the list chronologically by start date
            userHotelOrders.sort(Comparator.comparing(HotelOrder::getStartDate));

            model.addAttribute("hotelOrders", userHotelOrders);
        } else {
            model.addAttribute("hotelOrders", List.of());
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
    @GetMapping("/profile")
    public String profilePage(@AuthenticationPrincipal OidcUser user, Model model) {
        model.addAttribute("isLoggedIn", user != null);

        if (user != null) {
            model.addAttribute("nickname", user.getAttribute("nickname"));
            model.addAttribute("email", user.getEmail());
            model.addAttribute("picture", user.getPicture());
        }

        model.addAttribute("title", "Profile");
        model.addAttribute("contentTemplate", "profile");
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