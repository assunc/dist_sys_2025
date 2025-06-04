package com.example.springsoap.Controller;

import com.example.springsoap.Model.Hotel;
import com.example.springsoap.Model.Reservation;
import com.example.springsoap.Model.Room;
import com.example.springsoap.Model.RoomReservation;
import com.example.springsoap.UserService;
import jakarta.xml.bind.SchemaOutputResolver;
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
import org.apache.commons.lang3.StringUtils;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Controller
//@RequestMapping("/scopedproxy")
public class BrokerController {
    private final UserService userService;
    private final RestTemplate restTemplate;
    private final XPath xpath;

    private Reservation reservation;

    String hotelSupplierUrl = "http://hotelsupplier.azurewebsites.net:80/ws";
    String soapRequestHead = """
       <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:web="http://foodmenu.io/gt/webservice">
           <soapenv:Header>
               <wsse:Security soapenv:mustUnderstand="1" xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd" xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">
                   <wsse:UsernameToken wsu:Id="UsernameToken-90EE12B980F2A1E63C16196236965155">
                       <wsse:Username>brokerApp</wsse:Username>
                       <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText">$2a$10$t39txwmagJ8611gPSQyJxeV5BlH6MQKLzxTRo4uKFDijXup0cUzAG</wsse:Password>
                   </wsse:UsernameToken>
               </wsse:Security>
           </soapenv:Header>
       """;
    HttpHeaders headers = new HttpHeaders();
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    @Autowired
    public BrokerController(UserService userService, RestTemplate restTemplate, Reservation reservation) {
        this.userService = userService;
        this.restTemplate = restTemplate;
        this.reservation = reservation;
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
        headers.setContentType(MediaType.TEXT_XML);
        factory.setNamespaceAware(true);
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

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // Manually construct SOAP request XML
        String soapRequest = soapRequestHead + String.format("""
                   <soapenv:Body>
                      <web:getFreeHotelsRequest>
                         <web:startDate>%s</web:startDate>
                         <web:endDate>%s</web:endDate>
                      </web:getFreeHotelsRequest>
                   </soapenv:Body>
                </soapenv:Envelope>
                """,
                dateFormat.format(startDate), dateFormat.format(endDate)
        );

        List<Hotel> freeHotels = new ArrayList<>();

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(hotelSupplierUrl, new HttpEntity<>(soapRequest, headers), String.class);
            String responseBody = response.getBody();
            assert responseBody != null;

            // XPath to find all hotel elements (now using 'ns2' prefix)
            NodeList hotelNodes = (NodeList) xpath.evaluate("//ns2:hotels", factory.newDocumentBuilder().parse(new InputSource(new StringReader(responseBody))), XPathConstants.NODESET);

            for (int i = 0; i < hotelNodes.getLength(); i++) {
                org.w3c.dom.Element roomElement = (org.w3c.dom.Element) hotelNodes.item(i);
                String city = xpath.evaluate("ns2:city", roomElement);
                String country = xpath.evaluate("ns2:country", roomElement);
                if (destination.isEmpty() || StringUtils.containsIgnoreCase(city, destination) || StringUtils.containsIgnoreCase(country, destination)) {
                    freeHotels.add(new Hotel(
                            Integer.parseInt(xpath.evaluate("ns2:id", roomElement)),
                            xpath.evaluate("ns2:name", roomElement),
                            xpath.evaluate("ns2:address", roomElement),
                            city,
                            country,
                            xpath.evaluate("ns2:phoneNumber", roomElement),
                            xpath.evaluate("ns2:description", roomElement)
                    ));
                }
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
        String soapRequest = soapRequestHead + String.format("""
                   <soapenv:Body>
                      <web:getHotelRequest>
                         <web:hotelId>%d</web:hotelId>
                      </web:getHotelRequest>
                   </soapenv:Body>
                </soapenv:Envelope>
                """,
                hotelId
        );

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(hotelSupplierUrl, new HttpEntity<>(soapRequest, headers), String.class);
            String responseBody = response.getBody();

            // Parse the SOAP response
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
        String soapRequest2 = soapRequestHead + String.format("""
                   <soapenv:Body>
                      <web:getFreeRoomsRequest>
                         <web:startDate>%s</web:startDate>
                         <web:endDate>%s</web:endDate>
                         <web:hotelId>%d</web:hotelId>
                      </web:getFreeRoomsRequest>
                   </soapenv:Body>
                </soapenv:Envelope>
                """,
                dateFormat.format(startDate), dateFormat.format(endDate), hotelId
        );

        List<Room> filteredRooms = new ArrayList<>();
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(hotelSupplierUrl, new HttpEntity<>(soapRequest2, headers), String.class);
            String responseBody = response.getBody();

            // Parse the SOAP response
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

    @PostMapping("/payment")
    public String payment(@RequestParam Map<String, String> allRequestParams, @AuthenticationPrincipal OidcUser user, Model model) {
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Payment Information");
        model.addAttribute("contentTemplate", "payment");

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
        model.addAttribute("contentTemplate", "combo");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // Two stage commit
        boolean allBookingsPending = true;
        boolean allBookingsBooked = true;
        boolean allBookingsCanceled = false;

        List<Integer> bookingIds = new ArrayList<>();

        if (!reservation.getRoomReservations().isEmpty()) {
            // Manually construct SOAP request XML
            String soapRequest = soapRequestHead +"""
                <soapenv:Body>
                    <web:addBookingRequest>
                """;
            for (RoomReservation roomReservation : reservation.getRoomReservations()) {
                soapRequest += String.format("""
                   <web:booking>
                       <web:roomId>%d</web:roomId>
                       <web:startDate>%s</web:startDate>
                       <web:endDate>%s</web:endDate>
                   </web:booking>
                   """,
                        roomReservation.getRoom().getId(),
                        dateFormat.format(roomReservation.getStartDate()),
                        dateFormat.format(roomReservation.getEndDate())
                );
            }
            soapRequest += """
                   </web:addBookingRequest>
                   </soapenv:Body>
                </soapenv:Envelope>
                """;


            System.out.println(soapRequest);

            try {
                ResponseEntity<String> response = restTemplate.postForEntity(hotelSupplierUrl, new HttpEntity<>(soapRequest, headers), String.class);
                String responseBody = response.getBody();
                assert responseBody != null;

                Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(responseBody)));
                doc.getDocumentElement().normalize(); // Normalize the document for consistent parsing

                NodeList bookingIdNodes = doc.getElementsByTagName("bookingId");
                NodeList statusNodes = doc.getElementsByTagName("status");

                for (int i = 0; i < bookingIdNodes.getLength(); i++) {
                    try {
                        bookingIds.add(Integer.parseInt(bookingIdNodes.item(i).getTextContent()));
                    } catch (NumberFormatException e) {
                        System.err.println("Warning: Could not parse bookingId at index " + i + ": " + bookingIdNodes.item(i).getTextContent());
                    }
                }

                for (int i = 0; i < statusNodes.getLength(); i++) {
                    String statusText = statusNodes.item(i).getTextContent();
                    if (!statusText.trim().equalsIgnoreCase("pending")) {
                        allBookingsPending = false;
                        break;
                    }
                }
                model.addAttribute("error", false);

            } catch (Exception e) {
                System.err.println("Exception during booking search: " + e.getMessage());
                e.printStackTrace(); // Print full stack trace for detailed debugging
                model.addAttribute("error", "Error searching for hotels: " + e.getMessage());
            }
        }



        if (allBookingsPending) { // Stage 2
            if (!reservation.getRoomReservations().isEmpty()) {
                String soapRequest2 = soapRequestHead + """
                <soapenv:Body>
                    <web:confirmBookingRequest>
                """;
                for (int bookingId : bookingIds) {
                    soapRequest2 += String.format("""
                   <web:bookingId>%d</web:bookingId>
                   """,bookingId
                    );
                }
                soapRequest2 += """
                   </web:confirmBookingRequest>
                   </soapenv:Body>
                </soapenv:Envelope>
                """;

                System.out.println(soapRequest2);

                try {
                    ResponseEntity<String> response = restTemplate.postForEntity(hotelSupplierUrl, new HttpEntity<>(soapRequest2, headers), String.class);
                    String responseBody = response.getBody();
                    assert responseBody != null;

                    Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(responseBody)));
                    doc.getDocumentElement().normalize(); // Normalize the document for consistent parsing

                    NodeList statusNodes = doc.getElementsByTagName("status");

                    allBookingsBooked = statusNodes.item(0).getTextContent().trim().equalsIgnoreCase("booked");

                    model.addAttribute("error", false);

                } catch (Exception e) {
                    System.err.println("Exception during booking search: " + e.getMessage());
                    e.printStackTrace(); // Print full stack trace for detailed debugging
                    model.addAttribute("error", "Error searching for hotels: " + e.getMessage());
                }
            }
        }

        if (!allBookingsPending || !allBookingsBooked) { // Abort
            if (!reservation.getRoomReservations().isEmpty()) {
                String soapRequest3 = soapRequestHead + """
                <soapenv:Body>
                    <web:cancelBookingRequest>
                """;
                for (int bookingId : bookingIds) {
                    soapRequest3 += String.format("""
                   <web:bookingId>%d</web:bookingId>
                   """,bookingId
                    );
                }
                soapRequest3 += """
                   </web:cancelBookingRequest>
                   </soapenv:Body>
                </soapenv:Envelope>
                """;

                System.out.println(soapRequest3);

                try {
                    ResponseEntity<String> response = restTemplate.postForEntity(hotelSupplierUrl, new HttpEntity<>(soapRequest3, headers), String.class);
                    String responseBody = response.getBody();
                    assert responseBody != null;

                    Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(responseBody)));
                    doc.getDocumentElement().normalize(); // Normalize the document for consistent parsing

                    NodeList statusNodes = doc.getElementsByTagName("status");

                    allBookingsCanceled = statusNodes.item(0).getTextContent().trim().equalsIgnoreCase("canceled");

                    model.addAttribute("error", false);

                } catch (Exception e) {
                    System.err.println("Exception during booking search: " + e.getMessage());
                    e.printStackTrace(); // Print full stack trace for detailed debugging
                    model.addAttribute("error", "Error searching for hotels: " + e.getMessage());
                }
            }
        }

        if (allBookingsBooked) {
            // everything went well
        }

        if (allBookingsCanceled) {
            // transaction aborted
        }

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