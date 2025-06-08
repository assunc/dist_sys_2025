package com.example.springsoap.Controller;

import com.example.springsoap.Entities.FlightOrder;
import com.example.springsoap.Entities.Order;
import com.example.springsoap.Entities.User;
import com.example.springsoap.Model.Airline;
import com.example.springsoap.Model.Hotel;
import com.example.springsoap.Model.Room;
import com.example.springsoap.Model.Seat;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.client.RestTemplate;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Controller
public class BrokerController {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private FlightOrderRepository flightOrderRepository;
    @Autowired
    private UserRepository userRepository;

    private final UserService userService;
    private final RestTemplate restTemplate;
    private final XPath xpath;

    @Autowired
    public BrokerController(UserService userService, RestTemplate restTemplate) {
        this.userService = userService;
        this.restTemplate = restTemplate;
        xpath = XPathFactory.newInstance().newXPath();
        // Register a custom NamespaceContext to handle "ns2" and other namespaces
        xpath.setNamespaceContext(new javax.xml.namespace.NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                // Map "ns2" to the correct namespace URI from the response
                if (prefix.equals("ns2")) return "http://foodmenu.io/gt/webservice";
                if (prefix.equals("SOAP-ENV")) return "http://schemas.xmlsoap.org/soap/envelope/";
                return null;
            }
            public String getPrefix(String namespaceURI) { return null; }
            public java.util.Iterator<String> getPrefixes(String namespaceURI) { return null; } // Corrected type
        });
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
                .uri(new URI("http://localhost:8081/flights"))
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

        String uri = "http://localhost:8081/flights/searchByDateAndRoute?source=" + encodedSource +
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
                .uri(new URI("http://localhost:8081/flights/" + id))
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
                .uri(new URI("http://localhost:8081/seats/available?flightNumber=" + flightNumber + "&seatClass=" + classType))
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        List<Seat> seats = mapper.readValue(response.body(), new TypeReference<List<Seat>>() {});

        String seatsJson = mapper.writeValueAsString(seats);

        HttpRequest flightRequest = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8081/flights/" + flightNumber))
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
                    .uri(new URI("http://localhost:8081/seats/" + seatId + "/reserve"))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> reserveResponse = client.send(reserveRequest, HttpResponse.BodyHandlers.ofString());

            // 2b. Create booking
            HttpRequest bookingRequest = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8081/bookings?seatId=" + seatId + "&status=BOOKED"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> bookingResponse = client.send(bookingRequest, HttpResponse.BodyHandlers.ofString());

            // 2c. Extract bookingId
            JsonNode bookingJson = mapper.readTree(bookingResponse.body());
            long bookingId = bookingJson.get("id").asLong();

            // 2d. Get seat info from supplier
            HttpRequest seatRequest = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8081/seats/" + seatId))
                    .GET()
                    .build();
            HttpResponse<String> seatResponse = client.send(seatRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode seatJson = mapper.readTree(seatResponse.body());

            String seatNumber = seatJson.get("seatNumber").asText();
            long flightId = seatJson.get("flightNumber").asLong(); // adjust if field is nested

            // 2e. Get flight info from supplier
            HttpRequest flightRequest = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8081/flights/" + flightId))
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

    @GetMapping("/my-flight-orders")
    public String getUserFlightOrders(@AuthenticationPrincipal OidcUser oidcUser, Model model) throws IOException, InterruptedException, URISyntaxException {
        if (oidcUser == null) return "redirect:/login";

        String auth0Id = oidcUser.getSubject();
        Optional<User> optionalUser = userRepository.findByAuth0Id(auth0Id);
        if (optionalUser.isEmpty()) {
            model.addAttribute("error", "User not found.");
            return "error";
        }

        User currentUser = optionalUser.get();
        List<Order> orders = orderRepository.findByUser(currentUser);
        List<FlightOrder> allFlightOrders = flightOrderRepository.findByOrderIn(orders);

        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        for (FlightOrder flight : allFlightOrders) {
            String flightNum = flight.getFlightNumber();  // e.g., "BRU-NYC-001"
            // You need to map this to flight ID (or parse it out) if it's not stored directly

            // Assuming flight number is unique or convertible to ID:
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8081/flights/flightNumber/" + flightNum)) // or `/flights/{id}`
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode flightJson = mapper.readTree(resp.body());
                String departureTime = flightJson.get("departureTime").asText(); // e.g., 2025-06-10T15:45:00
                flight.setStatus(flight.getStatus() + " @ " + departureTime); // or setDepartureTime() if you added the field
            }
        }

        model.addAttribute("orders", orders);
        model.addAttribute("flightOrders", allFlightOrders);
        model.addAttribute("isLoggedIn", true);
        model.addAttribute("contentTemplate", "flight-orders");

        return "layout";
    }

    @PostMapping("/cancel-order")
    public String cancelFlightOrder(@RequestParam("flightOrderId") Integer flightOrderId,
                                    @AuthenticationPrincipal OidcUser user,
                                    Model model) throws IOException, InterruptedException, URISyntaxException {

        Optional<FlightOrder> optionalFlightOrder = flightOrderRepository.findById(flightOrderId);
        if (optionalFlightOrder.isEmpty()) {
            model.addAttribute("error", "Flight order not found.");
            return "error";
        }

        FlightOrder flightOrder = optionalFlightOrder.get();
        long bookingId = flightOrder.getBookingId();

        // Call supplier API to cancel booking
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8081/bookings/cancel?bookingId=" + bookingId))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Update broker DB
            flightOrder.setStatus("CANCELLED");
            flightOrderRepository.save(flightOrder);

            Order order = flightOrder.getOrder();
            order.setStatus("CANCELLED");
            orderRepository.save(order);

            return "redirect:/my-flight-orders";
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

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // Manually construct SOAP request XML
        String soapRequest = String.format(
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:web=\"http://foodmenu.io/gt/webservice\">\n" +
                "   <soapenv:Header>\n" +
                "       <wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n" +
                "           <wsse:UsernameToken wsu:Id=\"UsernameToken-90EE12B980F2A1E63C16196236965155\">\n" +
                "               <wsse:Username>brokerApp</wsse:Username>\n" +
                "               <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">$2a$10$t39txwmagJ8611gPSQyJxeV5BlH6MQKLzxTRo4uKFDijXup0cUzAG</wsse:Password>\n" +
                "           </wsse:UsernameToken>\n" +
                "       </wsse:Security>\n" +
                "   </soapenv:Header>\n" +
                "   <soapenv:Body>\n" +
                "      <web:getFreeHotelsRequest>\n" +
                "         <web:startDate>%s</web:startDate>\n" +
                "         <web:endDate>%s</web:endDate>\n" +
                "      </web:getFreeHotelsRequest>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>",
                dateFormat.format(startDate), dateFormat.format(endDate)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);

        List<Hotel> freeHotels = new ArrayList<>();
        String hotelSupplierUrl = "http://hotelsupplier.azurewebsites.net:80/ws";

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(hotelSupplierUrl, new HttpEntity<>(soapRequest, headers), String.class);
            String responseBody = response.getBody();
            assert responseBody != null;

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // XPath to find all hotel elements (now using 'ns2' prefix)
            NodeList hotelNodes = (NodeList) xpath.evaluate("//ns2:hotels", factory.newDocumentBuilder().parse(new InputSource(new StringReader(responseBody))), XPathConstants.NODESET);

            for (int i = 0; i < hotelNodes.getLength(); i++) {
                org.w3c.dom.Element roomElement = (org.w3c.dom.Element) hotelNodes.item(i);
                freeHotels.add(new Hotel(
                        Integer.parseInt(xpath.evaluate("ns2:id", roomElement)),
                        xpath.evaluate("ns2:name", roomElement),
                        xpath.evaluate("ns2:address", roomElement),
                        xpath.evaluate("ns2:city", roomElement),
                        xpath.evaluate("ns2:country", roomElement),
                        xpath.evaluate("ns2:phoneNumber", roomElement),
                        xpath.evaluate("ns2:description", roomElement)
                ));
            }
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
        // Initialize rooms list to empty when first loading the page

        // Manually construct SOAP request XML
        String soapRequest = String.format(
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:web=\"http://foodmenu.io/gt/webservice\">\n" +
                "   <soapenv:Header>\n" +
                "       <wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n" +
                "           <wsse:UsernameToken wsu:Id=\"UsernameToken-90EE12B980F2A1E63C16196236965155\">\n" +
                "               <wsse:Username>brokerApp</wsse:Username>\n" +
                "               <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">$2a$10$t39txwmagJ8611gPSQyJxeV5BlH6MQKLzxTRo4uKFDijXup0cUzAG</wsse:Password>\n" +
                "           </wsse:UsernameToken>\n" +
                "       </wsse:Security>\n" +
                "   </soapenv:Header>\n" +
                "   <soapenv:Body>\n" +
                "      <web:getHotelRequest>\n" +
                "         <web:hotelId>%d</web:hotelId>\n" +
                "      </web:getHotelRequest>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>",
                hotelId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);

        String hotelSupplierUrl = "http://hotelsupplier.azurewebsites.net:80/ws";

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(hotelSupplierUrl, new HttpEntity<>(soapRequest, headers), String.class);
            String responseBody = response.getBody();

            // Parse the SOAP response
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            assert responseBody != null;

            // XPath to find all hotel elements (now using 'ns2' prefix)
            NodeList hotelNodes = (NodeList) xpath.evaluate("//ns2:hotel", factory.newDocumentBuilder().parse(new InputSource(new StringReader(responseBody))), XPathConstants.NODESET);

            for (int i = 0; i < hotelNodes.getLength(); i++) {
                org.w3c.dom.Element roomElement = (org.w3c.dom.Element) hotelNodes.item(i);
                Hotel hotel = new Hotel(
                        Integer.parseInt(xpath.evaluate("ns2:id", roomElement)),
                        xpath.evaluate("ns2:name", roomElement),
                        xpath.evaluate("ns2:address", roomElement),
                        xpath.evaluate("ns2:city", roomElement),
                        xpath.evaluate("ns2:country", roomElement),
                        xpath.evaluate("ns2:phoneNumber", roomElement),
                        xpath.evaluate("ns2:description", roomElement)
                );
                model.addAttribute("hotel", hotel);
            }
            model.addAttribute("error", false);
        } catch (Exception e) {
            System.err.println("Exception during hotel search: " + e.getMessage());
            e.printStackTrace(); // Print full stack trace for detailed debugging
            model.addAttribute("error", "Error searching for hotels: " + e.getMessage());
            model.addAttribute("hotel", null); // Ensure rooms list is empty on error
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // Manually construct SOAP request XML
        String soapRequest2 = String.format(
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:web=\"http://foodmenu.io/gt/webservice\">\n" +
                        "   <soapenv:Header>\n" +
                        "       <wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n" +
                        "           <wsse:UsernameToken wsu:Id=\"UsernameToken-90EE12B980F2A1E63C16196236965155\">\n" +
                        "               <wsse:Username>brokerApp</wsse:Username>\n" +
                        "               <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">$2a$10$t39txwmagJ8611gPSQyJxeV5BlH6MQKLzxTRo4uKFDijXup0cUzAG</wsse:Password>\n" +
                        "           </wsse:UsernameToken>\n" +
                        "       </wsse:Security>\n" +
                        "   </soapenv:Header>\n" +
                        "   <soapenv:Body>\n" +
                        "      <web:getFreeRoomsRequest>\n" +
                        "         <web:startDate>%s</web:startDate>\n" +
                        "         <web:endDate>%s</web:endDate>\n" +
                        "         <web:hotelId>%d</web:hotelId>\n" +
                        "      </web:getFreeRoomsRequest>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>",
                dateFormat.format(startDate), dateFormat.format(endDate), hotelId
        );

        List<Room> filteredRooms = new ArrayList<>();
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(hotelSupplierUrl, new HttpEntity<>(soapRequest2, headers), String.class);
            String responseBody = response.getBody();

            // Parse the SOAP response
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            assert responseBody != null;

            // XPath to find all hotel elements (now using 'ns2' prefix)
            NodeList roomNodes = (NodeList) xpath.evaluate("//ns2:rooms", factory.newDocumentBuilder().parse(new InputSource(new StringReader(responseBody))), XPathConstants.NODESET);

            for (int i = 0; i < roomNodes.getLength(); i++) {
                org.w3c.dom.Element roomElement = (org.w3c.dom.Element) roomNodes.item(i);
                int roomNOfPeople = Integer.parseInt(xpath.evaluate("ns2:nOfPeople", roomElement));

                if (roomNOfPeople >= numberOfPeople) {
                    filteredRooms.add(new Room(
                            Integer.parseInt(xpath.evaluate("ns2:roomId", roomElement)),
                            Integer.parseInt(xpath.evaluate("ns2:number", roomElement)),
                            roomNOfPeople,
                            Integer.parseInt(xpath.evaluate("ns2:price", roomElement))
                    ));
                }
            }
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

    @GetMapping("/combo")
    public String combo(@AuthenticationPrincipal OidcUser user, Model model) {
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Hotels + Flights");
        model.addAttribute("contentTemplate", "combo");
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