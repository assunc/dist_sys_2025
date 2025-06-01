package com.example.springsoap.Controller;

import com.example.springsoap.UserService;
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
import org.springframework.web.client.RestTemplate;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

// Define a simple Room class as POJO to hold parsed data
class Room {
    private int number;
    private int nOfPeople;
    private int price;

    public Room(int number, int nOfPeople, int price) {
        this.number = number;
        this.nOfPeople = nOfPeople;
        this.price = price;
    }

    // Getters for Thymeleaf
    public int getNumber() {
        return number;
    }

    public int getNOfPeople() {
        return nOfPeople;
    }

    public int getPrice() {
        return price;
    }
}

@Controller
public class BrokerController {
    private final UserService userService;
    private final RestTemplate restTemplate;

    @Autowired
    public BrokerController(UserService userService, RestTemplate restTemplate) {
        this.userService = userService;
        this.restTemplate = restTemplate;
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
        model.addAttribute("rooms", List.of());
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
        String formattedStartDate = dateFormat.format(startDate);
        String formattedEndDate = dateFormat.format(endDate);

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
                        "      <web:getFreeRoomsRequest>\n" +
                        "         <web:startDate>%s</web:startDate>\n" +
                        "         <web:endDate>%s</web:endDate>\n" +
                        "      </web:getFreeRoomsRequest>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>",
                formattedStartDate, formattedEndDate
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.set("SOAPAction", "http://foodmenu.io/gt/webservice/getFreeRoomsRequest"); // This SOAPAction is crucial

        HttpEntity<String> entity = new HttpEntity<>(soapRequest, headers);

        List<Room> filteredRooms = new ArrayList<>();
        String hotelSupplierUrl = "http://hotelsupplier.azurewebsites.net:80/ws";

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(hotelSupplierUrl, entity, String.class);
            String responseBody = response.getBody();
            System.out.println("Raw SOAP Response Body:\n" + responseBody);

            // Parse the SOAP response
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Important for XPath to work correctly with namespaces
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(responseBody)));

            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();

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

            // XPath to find all room elements (now using 'ns2' prefix)
            NodeList roomNodes = (NodeList) xpath.evaluate("//ns2:rooms", doc, XPathConstants.NODESET);
            System.out.println("Number of roomNodes found by XPath: " + roomNodes.getLength());

            for (int i = 0; i < roomNodes.getLength(); i++) {
                org.w3c.dom.Element roomElement = (org.w3c.dom.Element) roomNodes.item(i);
                // Accessing child elements using 'ns2' prefix
                String roomNumberStr = xpath.evaluate("ns2:number", roomElement);
                String roomNOfPeopleStr = xpath.evaluate("ns2:nOfPeople", roomElement);
                String roomPriceStr = xpath.evaluate("ns2:price", roomElement);

                // Basic validation for parsed strings before converting to int
                if (roomNumberStr.isEmpty() || roomNOfPeopleStr.isEmpty() || roomPriceStr.isEmpty()) {
                    System.err.println("Warning: Empty string encountered when parsing room details. Skipping this room.");
                    continue; // Skip this room if any part is empty
                }

                int roomNumber = Integer.parseInt(roomNumberStr);
                int roomNOfPeople = Integer.parseInt(roomNOfPeopleStr);
                int roomPrice = Integer.parseInt(roomPriceStr);

                System.out.println("Parsed Room - Number: " + roomNumber + ", People: " + roomNOfPeople + ", Price: " + roomPrice);

                // Apply filtering logic
                if (roomNOfPeople >= numberOfPeople) {
                    filteredRooms.add(new Room(roomNumber, roomNOfPeople, roomPrice));
                }
            }
            System.out.println("Number of filteredRooms after applying criteria: " + filteredRooms.size());

            model.addAttribute("rooms", filteredRooms);
            model.addAttribute("searchPerformed", true);
            model.addAttribute("hasRooms", !filteredRooms.isEmpty());

        } catch (Exception e) {
            System.err.println("Exception during hotel search: " + e.getMessage());
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