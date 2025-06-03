package com.example.springsoap.Controller;

import com.example.springsoap.Model.Airline;
import com.example.springsoap.Model.Hotel;
import com.example.springsoap.Model.Room;
import com.example.springsoap.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
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

@Controller
public class BrokerController {
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

        // Deserialize response into a list of Airline DTOs
        List<Airline> flights = mapper.readValue(json, new TypeReference<List<Airline>>() {});

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





    @GetMapping("/flight-info/{id}")
    public String flightDetails(@PathVariable Long id, Model model) throws IOException, InterruptedException, URISyntaxException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8081/flights/" + id))
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        Airline flight = mapper.readValue(response.body(), Airline.class);
        model.addAttribute("flight", flight);
        return "flight-info";
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