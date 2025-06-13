package com.example.springsoap;

import com.example.springsoap.Model.FlightReservation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import com.example.springsoap.Entities.Order;
import com.example.springsoap.Entities.HotelOrder;
import com.example.springsoap.Entities.FlightOrder;
import com.example.springsoap.Model.OrderProcessingMessage;
import com.example.springsoap.Repository.OrderRepository;
import com.example.springsoap.Repository.HotelOrderRepository;
import com.example.springsoap.Repository.FlightOrderRepository;


@Component
public class OrderProcessingConsumer {

    private final OrderRepository orderRepository;
    private final HotelOrderRepository hotelOrderRepository;
    private final FlightOrderRepository flightOrderRepository;
    private final HotelService hotelService;
    private final FlightService flightService;
    private final ObjectMapper objectMapper;
    private final String orderInitiationQueueName; // To dynamically build DLQ name

    // Inject dependencies, including the queue name for DLQ construction
    public OrderProcessingConsumer(OrderRepository orderRepository, HotelOrderRepository hotelOrderRepository,
                                   FlightOrderRepository flightOrderRepository, HotelService hotelService,
                                   FlightService flightService, ObjectMapper objectMapper,
                                   @Value("${azure.servicebus.queue-name}") String orderInitiationQueueName) {
        this.orderRepository = orderRepository;
        this.hotelOrderRepository = hotelOrderRepository;
        this.flightOrderRepository = flightOrderRepository;
        this.hotelService = hotelService;
        this.flightService = flightService;
        this.objectMapper = objectMapper;
        this.orderInitiationQueueName = orderInitiationQueueName;
    }

    // Listener for initial order processing messages from the Service Bus Queue
    @JmsListener(destination = "${azure.servicebus.queue-name}")
    @Transactional
    public void processOrderInitiation(String messageJson) { // Receive as String (JSON)
        OrderProcessingMessage message;
        try {
            message = objectMapper.readValue(messageJson, OrderProcessingMessage.class);
        } catch (Exception e) {
            System.err.println("Error deserializing message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to deserialize message", e); // Service Bus retries
        }

        System.out.println("Received order initiation message for Order ID: " + message.getOrderId());

        Optional<Order> optionalOrder = orderRepository.findById(message.getOrderId());
        if (optionalOrder.isEmpty()) {
            System.err.println("Order not found for ID: " + message.getOrderId() + ". Dropping message.");
            return;
        }
        Order order = optionalOrder.get();

        List<HotelOrder> hotelOrders = hotelOrderRepository.findByOrder(order);
        List<FlightOrder> flightOrders = flightOrderRepository.findByOrder(order);

        // --- Phase 1: Reserve ---
        boolean allPending = true;
        boolean allConfirmed = true;

        if (order.getStatus().equalsIgnoreCase("processing")) {
            if (!message.getReservation().getRoomReservations().isEmpty() && hotelOrders.isEmpty()) { // reserve didnt go through
                try {
                    allPending = hotelService.reserveOrders(message.getReservation().getRoomReservations(), order, hotelOrders);
                } catch (Exception e) {
                    System.err.println("Exception during booking search: " + e.getMessage());
                    e.printStackTrace();
                    allPending = false;
                }
            }

            if (!message.getReservation().getFlightReservations().isEmpty() && flightOrders.isEmpty()) { // reserve didnt go through
                List<Long> seatIds = message.getReservation().getFlightReservations().stream().map(FlightReservation::getSeatId).toList();
                try {
                    allPending &= flightService.reserveSeats(seatIds, order, flightOrders);
                } catch (Exception e) {
                    System.err.println("Flight reservation error: " + e.getMessage());
                    e.printStackTrace();
                    allPending = false;
                }
            }
        }

        // --- Phase 2: Confirm if all reserved, else try to cancel ---
        if (allPending) {
            order.setStatus("pending");
            // save so if it fails it can start from here next retry
            orderRepository.save(order);
            hotelOrderRepository.saveAll(hotelOrders);
            flightOrderRepository.saveAll(flightOrders);

            if (!hotelOrders.isEmpty()) {
                try {
                    allConfirmed = hotelService.confirmOrders(order, hotelOrders);
                } catch (Exception e) {
                    System.err.println("Exception during booking search: " + e.getMessage());
                    e.printStackTrace();
                    allConfirmed = false;
                }
            }

            if (!flightOrders.isEmpty()) {
                try {
                    allConfirmed &= flightService.confirmSeats(order, flightOrders);
                } catch (Exception e) {
                    System.err.println("Flight confirmation error: " + e.getMessage());
                    e.printStackTrace();
                    allConfirmed = false;
                }
            }


            if (allConfirmed) {
                order.setStatus("booked");
                orderRepository.save(order);
                hotelOrderRepository.saveAll(hotelOrders);
                flightOrderRepository.saveAll(flightOrders);
                System.out.println("Order " + order.getId() + " successfully BOOKED.");
            }
        }
        if (!allPending || !allConfirmed){
            System.out.println("Order " + order.getId() + " not fully confirmed. Will retry or cancel.");
            throw new RuntimeException("Partial confirmation, need retry."); // Trigger rollback and re-queue
        }
    }

    @JmsListener(destination = "${azure.servicebus.queue-name}/$DeadLetterQueue")
    public void handleDeadLetterMessage(String messageJson) { // Receive as String (JSON)
        OrderProcessingMessage message;
        try {
            message = objectMapper.readValue(messageJson, OrderProcessingMessage.class);
        } catch (Exception e) {
            System.err.println("Error deserializing dead-letter message: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        Optional<Order> optionalOrder = orderRepository.findById(message.getOrderId());
        if (optionalOrder.isEmpty()) {
            System.err.println("Order not found for ID: " + message.getOrderId() + ". Dropping message.");
            return;
        }
        Order order = optionalOrder.get();
        List<HotelOrder> hotelOrders = hotelOrderRepository.findByOrder(order);
        List<FlightOrder> flightOrders = flightOrderRepository.findByOrder(order);

        System.out.println("Order " + order.getId() + " ran out of time. Cancelling.");
        boolean allCanceled = true;

        // Hotel cancel
        if (!message.getReservation().getRoomReservations().isEmpty()) {
            try {
                allCanceled = hotelService.cancelOrders(order, hotelOrders, true);
            } catch (Exception e) {
                System.err.println("Hotel cancellation error: " + e.getMessage());
                e.printStackTrace();
                allCanceled = false;
            }
        }

        // Flight cancel
        if (!flightOrders.isEmpty()) {
            try {
                allCanceled &= flightService.cancelBookings(flightOrders);
            } catch (Exception e) {
                System.err.println("Flight cancellation error: " + e.getMessage());
                e.printStackTrace();
                allCanceled = false;
            }
        }

        order.setStatus("canceled");
        if (allCanceled) {
            System.out.println("Order " + order.getId() + " fully CANCELED due to reservation failure.");
        } else {
            System.out.println("Order " + order.getId() + " could not be fully cancelled. It will be auto canceled in the supplier");
            for (HotelOrder ho : hotelOrders) {
                ho.setStatus("canceled");
            }
            for (FlightOrder fo : flightOrders) {
                fo.setStatus("canceled");
            }
        }
        orderRepository.save(order);
        hotelOrderRepository.saveAll(hotelOrders);
        flightOrderRepository.saveAll(flightOrders);
    }
}