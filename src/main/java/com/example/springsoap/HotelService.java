package com.example.springsoap;

import com.example.springsoap.Entities.HotelOrder;
import com.example.springsoap.Entities.Order;
import com.example.springsoap.Model.Hotel;
import com.example.springsoap.Model.Reservation;
import com.example.springsoap.Model.Room;
import com.example.springsoap.Model.RoomReservation;
import com.example.springsoap.Repository.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Map;

@Service
public class HotelService {

    private final RestTemplate restTemplate;
    private final XPath xpath;

    private final HotelSupplierRepository hotelSupplierRepository;
    private final HotelOrderRepository hotelOrderRepository;
    private final OrderRepository orderRepository;

    String hotelSupplierUrl = "http://hotelsupplier.azurewebsites.net:80/ws";
    String hotelSupplierNamespaceURI = "http://foodmenu.io/gt/webservice";
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
    SimpleDateFormat dateFormat;


    public HotelService(
            RestTemplate restTemplate, HotelSupplierRepository hotelSupplierRepository,
            HotelOrderRepository hotelOrderRepository,
            OrderRepository orderRepository
    ) {
        this.restTemplate = restTemplate;
        this.hotelSupplierRepository = hotelSupplierRepository;
        this.hotelOrderRepository = hotelOrderRepository;
        this.orderRepository = orderRepository;
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
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    }

    public List<Hotel> getFreeHotels(Date startDate, Date endDate, String destination) throws Exception {
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
                        xpath.evaluate("ns2:description", roomElement),
                        xpath.evaluate("ns2:imageUrl", roomElement)
                ));
            }
        }
        return freeHotels;
    }

    public Hotel getHotel(int hotelId) throws Exception {
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

        ResponseEntity<String> response = restTemplate.postForEntity(hotelSupplierUrl, new HttpEntity<>(soapRequest, headers), String.class);
        String responseBody = response.getBody();
        assert responseBody != null;

        // XPath to find all hotel elements (now using 'ns2' prefix)
        NodeList hotelNodes = (NodeList) xpath.evaluate("//ns2:hotel", factory.newDocumentBuilder().parse(new InputSource(new StringReader(responseBody))), XPathConstants.NODESET);
        org.w3c.dom.Element roomElement = (org.w3c.dom.Element) hotelNodes.item(0);
        return new Hotel(
                Integer.parseInt(xpath.evaluate("ns2:id", roomElement)),
                xpath.evaluate("ns2:name", roomElement),
                xpath.evaluate("ns2:address", roomElement),
                xpath.evaluate("ns2:city", roomElement),
                xpath.evaluate("ns2:country", roomElement),
                xpath.evaluate("ns2:phoneNumber", roomElement),
                xpath.evaluate("ns2:description", roomElement),
                xpath.evaluate("ns2:imageUrl", roomElement)
        );
    }

    public List<Room> getFreeRooms(int hotelId, Date startDate, Date endDate, int numberOfPeople) throws Exception {
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
        ResponseEntity<String> response = restTemplate.postForEntity(hotelSupplierUrl, new HttpEntity<>(soapRequest2, headers), String.class);
        String responseBody = response.getBody();
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
        return filteredRooms;
    }

    public boolean reserveOrders(List<RoomReservation> roomReservations, Order order, List<HotelOrder> hotelOrders) throws Exception {
        String soapRequest = soapRequestHead +"""
                <soapenv:Body>
                    <web:addBookingRequest>
                """;
        for (RoomReservation roomReservation : roomReservations) {
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

        boolean allBookingsPending = true;
        ResponseEntity<String> response = restTemplate.postForEntity(hotelSupplierUrl, new HttpEntity<>(soapRequest, headers), String.class);
        String responseBody = response.getBody();
        assert responseBody != null;
        System.out.println(responseBody);

        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(responseBody)));
        doc.getDocumentElement().normalize(); // Normalize the document for consistent parsing

        NodeList bookingIdNodes = doc.getElementsByTagNameNS(hotelSupplierNamespaceURI, "bookingId");
        NodeList statusNodes = doc.getElementsByTagNameNS(hotelSupplierNamespaceURI, "status");

        for (int i = 0; i < bookingIdNodes.getLength(); i++) {
            try {
                HotelOrder newHotelOrder = new HotelOrder(
                        order,
                        LocalDate.ofInstant(roomReservations.get(i).getStartDate().toInstant(), ZoneId.systemDefault()),
                        LocalDate.ofInstant(roomReservations.get(i).getEndDate().toInstant(), ZoneId.systemDefault()),
                        Integer.parseInt(bookingIdNodes.item(i).getTextContent()),
                        statusNodes.item(i).getTextContent(),
                        hotelSupplierRepository.getReferenceById(1),
                        roomReservations.get(i).getRoom().getId(),
                        roomReservations.get(i).getHotelName(),
                        roomReservations.get(i).getRoom().getNumber()
                );
                hotelOrders.add(newHotelOrder);
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
        return allBookingsPending;
    }

    public boolean confirmOrders(Order order, List<HotelOrder> hotelOrders) throws Exception {
        String soapRequest = soapRequestHead + """
                <soapenv:Body>
                    <web:confirmBookingRequest>
                """;
        for (HotelOrder hotelOrder : hotelOrders) {
            soapRequest += String.format("""
                   <web:bookingId>%d</web:bookingId>
                   """,hotelOrder.getBookingId()
            );
        }
        soapRequest += """
                   </web:confirmBookingRequest>
                   </soapenv:Body>
                </soapenv:Envelope>
                """;
        System.out.println(soapRequest);

        ResponseEntity<String> response = restTemplate.postForEntity(hotelSupplierUrl, new HttpEntity<>(soapRequest, headers), String.class);
        String responseBody = response.getBody();
        assert responseBody != null;
        System.out.println(responseBody);

        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(responseBody)));
        doc.getDocumentElement().normalize(); // Normalize the document for consistent parsing

        NodeList statusNodes = doc.getElementsByTagNameNS(hotelSupplierNamespaceURI, "status");
        String status = statusNodes.item(0).getTextContent().trim();
        boolean allBookingsBooked = status.equalsIgnoreCase("booked");

        if (allBookingsBooked) {
            for (HotelOrder hotelOrder : hotelOrders) {
                hotelOrder.setStatus(status);
            }
            order.setStatus(status);
        }
        return allBookingsBooked;
    }

    public boolean cancelOrders(Order order, List<HotelOrder> hotelOrders, Boolean cancelEntireOrder) throws Exception {
        String soapRequest = soapRequestHead + """
                <soapenv:Body>
                    <web:cancelBookingRequest>
                """;
        for (HotelOrder hotelOrder : hotelOrders) {
            soapRequest += String.format("""
                   <web:bookingId>%d</web:bookingId>
                   """,hotelOrder.getBookingId()
            );
        }
        soapRequest += """
                   </web:cancelBookingRequest>
                   </soapenv:Body>
                </soapenv:Envelope>
                """;

        System.out.println(soapRequest);

        ResponseEntity<String> response = restTemplate.postForEntity(hotelSupplierUrl, new HttpEntity<>(soapRequest, headers), String.class);
        String responseBody = response.getBody();
        assert responseBody != null;
        System.out.println(responseBody);

        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(responseBody)));
        doc.getDocumentElement().normalize(); // Normalize the document for consistent parsing

        NodeList statusNodes = doc.getElementsByTagNameNS(hotelSupplierNamespaceURI, "status");
        String status = statusNodes.item(0).getTextContent().trim();
        boolean allBookingsCanceled = status.equalsIgnoreCase("canceled");

        if (allBookingsCanceled) {
            for (HotelOrder hotelOrder : hotelOrders) {
                hotelOrder.setStatus(status);
            }
            if(cancelEntireOrder) {
                order.setStatus(status);
            }

        }
        return allBookingsCanceled;
    }

    public void addHotelReservations(Map<String, String> allRequestParams, Reservation reservation) {
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
                    Room room = new Room(entry.getKey());
                    if (reservation.getRoomReservations().stream()
                            .filter((roomRes) -> roomRes.getRoom().getId() == room.getId())
                            .filter((roomRes) -> roomRes.getStartDate().before(endDate) && roomRes.getEndDate().after(startDate))
                            .toList().isEmpty()) { // check if there are no overlapping reservation in the shopping cart
                        reservation.addRoomReservation(new RoomReservation(
                                room,
                                allRequestParams.get("name"),
                                startDate, endDate
                        ));
                    }
                }
            }
        }
    }
}
