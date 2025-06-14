package com.example.springsoap;

import com.example.springsoap.Model.FlightReservation;
import com.example.springsoap.Model.OrderProcessingMessage;
import com.example.springsoap.Entities.Order;
import com.example.springsoap.Entities.HotelOrder;
import com.example.springsoap.Entities.FlightOrder;
import com.example.springsoap.Repository.OrderRepository;
import com.example.springsoap.Repository.HotelOrderRepository;
import com.example.springsoap.Repository.FlightOrderRepository;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.DisposableBean; // For client lifecycle management
import org.springframework.beans.factory.InitializingBean; // For client lifecycle management
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Keep for database transactions

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer; // For message and error handlers

@Component
public class OrderProcessingConsumer implements InitializingBean, DisposableBean { // Implement InitializingBean, DisposableBean

    private final OrderRepository orderRepository;
    private final HotelOrderRepository hotelOrderRepository;
    private final FlightOrderRepository flightOrderRepository;
    private final HotelService hotelService;
    private final FlightService flightService;
    private final ObjectMapper objectMapper;

    private final String connectionString;
    private final String queueName;
    private ServiceBusProcessorClient mainQueueProcessorClient;
    private ServiceBusProcessorClient deadLetterQueueProcessorClient;


    public OrderProcessingConsumer(OrderRepository orderRepository, HotelOrderRepository hotelOrderRepository,
                                   FlightOrderRepository flightOrderRepository, HotelService hotelService,
                                   FlightService flightService, ObjectMapper objectMapper,
                                   @Value("${azure.servicebus.connection-string}") String connectionString,
                                   @Value("${azure.servicebus.queue-name}") String queueName) {
        this.orderRepository = orderRepository;
        this.hotelOrderRepository = hotelOrderRepository;
        this.flightOrderRepository = flightOrderRepository;
        this.hotelService = hotelService;
        this.flightService = flightService;
        this.objectMapper = objectMapper;
        this.connectionString = connectionString;
        this.queueName = queueName;
    }

    // Listener for initial order processing messages from the Service Bus Queue
    @Transactional
    public void processOrderInitiationMessage(ServiceBusReceivedMessage message) {
        OrderProcessingMessage orderMessage;
        try {
            orderMessage = objectMapper.readValue(message.getBody().toString(), OrderProcessingMessage.class);
        } catch (Exception e) {
            System.err.println("Error deserializing message: " + e.getMessage());
            e.printStackTrace();
            return; // message cant be read
        }

        System.out.println("Received order initiation message for Order ID: " + orderMessage.getOrderId());

        Optional<Order> optionalOrder = orderRepository.findById(orderMessage.getOrderId());
        if (optionalOrder.isEmpty()) {
            System.err.println("Order not found for ID: " + orderMessage.getOrderId() + ". Dropping message.");
            return;
        }
        Order order = optionalOrder.get();
        if (orderMessage.getReservation().getRoomReservations().isEmpty() && orderMessage.getReservation().getFlightReservations().isEmpty()) {
            System.out.println("Order was empty. Dropping message.");
            return;
        }

        List<HotelOrder> hotelOrders = hotelOrderRepository.findByOrder(order);
        List<FlightOrder> flightOrders = flightOrderRepository.findByOrder(order);

        // --- Phase 1: Reserve ---
        boolean allPending = false;
        boolean allConfirmed = false;

        boolean hotelPending = true;
        boolean airlinePending = true;

        if (order.getStatus().equalsIgnoreCase("processing")) {
            System.out.println("Order was processing, send pending reservations");
            if (!orderMessage.getReservation().getRoomReservations().isEmpty() && hotelOrders.isEmpty()) { // reserve didnt go through
                try {
                    hotelPending = hotelService.reserveOrders(orderMessage.getReservation().getRoomReservations(), order, hotelOrders);
                } catch (Exception e) {
                    System.err.println("Exception during booking search: " + e.getMessage());
                    e.printStackTrace();
                    hotelPending = false;
                }
            }

            if (!orderMessage.getReservation().getFlightReservations().isEmpty() && flightOrders.isEmpty()) { // reserve didnt go through
                List<Long> seatIds = orderMessage.getReservation().getFlightReservations().stream().map(FlightReservation::getSeatId).toList();
                try {
                    hotelPending = flightService.reserveSeats(seatIds, order, flightOrders);
                } catch (Exception e) {
                    System.err.println("Flight reservation error: " + e.getMessage());
                    e.printStackTrace();
                    hotelPending = false;
                }
            }
            allPending = hotelPending && airlinePending;

            if (allPending) order.setStatus("pending");
            // save so if it fails it can start from here next retry
            orderRepository.save(order);
            hotelOrderRepository.saveAll(hotelOrders);
            flightOrderRepository.saveAll(flightOrders);
        }

        // --- Phase 2: Confirm if all reserved, else try to cancel ---
        if (allPending || order.getStatus().equalsIgnoreCase("pending")) {
            boolean hotelConfirmed = true;
            boolean airlineConfirmed = true;
            System.out.println("Order is processing, send confirmations");
            if (!hotelOrders.isEmpty()) {
                try {
                    hotelConfirmed = hotelService.confirmOrders(order, hotelOrders);
                } catch (Exception e) {
                    System.err.println("Exception during booking search: " + e.getMessage());
                    e.printStackTrace();
                    hotelConfirmed = false;
                }
            }

            if (!flightOrders.isEmpty()) {
                try {
                    airlineConfirmed = flightService.confirmSeats(order, flightOrders);
                } catch (Exception e) {
                    System.err.println("Flight confirmation error: " + e.getMessage());
                    e.printStackTrace();
                    airlineConfirmed = false;
                }
            }
            allConfirmed = hotelConfirmed && airlineConfirmed;

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

    private Consumer<ServiceBusErrorContext> processMainQueueError() {
        return errorContext -> {
            System.err.printf("Error occurred for resource: %s. Error type: %s%n",
                    errorContext.getEntityPath(), errorContext.getErrorSource());
            Throwable exception = errorContext.getException();
            System.err.printf("Error: %s%n", exception.getMessage());
            if (exception instanceof ServiceBusException) {
                ServiceBusException serviceBusException = (ServiceBusException) exception;
                if (serviceBusException.isTransient()) { // Now safe to call isTransient()
                    System.err.println("Error is transient/retriable.");
                } else {
                    System.err.println("Error is non-transient/non-retriable.");
                }
            } else {
                System.err.println("Error is a non-ServiceBusException type.");
                exception.printStackTrace();
            }
        };
    }

    @Transactional
    public void processDeadLetterMessage(ServiceBusReceivedMessage message) {
        OrderProcessingMessage orderMessage;
        try {
            orderMessage = objectMapper.readValue(message.getBody().toString(), OrderProcessingMessage.class);
        } catch (Exception e) {
            System.err.println("Error deserializing dead-letter message: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        Optional<Order> optionalOrder = orderRepository.findById(orderMessage.getOrderId());
        if (optionalOrder.isEmpty()) {
            System.err.println("Order not found for ID: " + orderMessage.getOrderId() + ". Dropping message.");
            return;
        }
        Order order = optionalOrder.get();
        List<HotelOrder> hotelOrders = hotelOrderRepository.findByOrder(order);
        List<FlightOrder> flightOrders = flightOrderRepository.findByOrder(order);

        System.out.println("Order " + order.getId() + " ran out of time. Cancelling.");
        boolean allCanceled = false;
        boolean hotelCanceled = true;
        boolean airlineCanceled = true;

        // Hotel cancel
        if (!orderMessage.getReservation().getRoomReservations().isEmpty()) {
            try {
                hotelCanceled = hotelService.cancelOrders(order, hotelOrders, true);
            } catch (Exception e) {
                System.err.println("Hotel cancellation error: " + e.getMessage());
                e.printStackTrace();
                hotelCanceled = false;
            }
        }

        // Flight cancel
        if (!flightOrders.isEmpty()) {
            try {
                airlineCanceled = flightService.cancelBookings(flightOrders);
            } catch (Exception e) {
                System.err.println("Flight cancellation error: " + e.getMessage());
                e.printStackTrace();
                airlineCanceled = false;
            }
        }
        allCanceled = hotelCanceled && airlineCanceled;

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

    // --- Error Handling for Dead Letter Queue Processor ---
    private Consumer<ServiceBusErrorContext> processDeadLetterQueueError() {
        return errorContext -> {
            System.err.printf("Error occurred in DLQ processor for resource: %s. Error type: %s%n",
                    errorContext.getEntityPath(), errorContext.getErrorSource());
            System.err.printf("Error: %s%n", errorContext.getException().getMessage());
        };
    }

    // --- Lifecycle Methods for ServiceBusProcessorClient ---
    @Override
    public void afterPropertiesSet() throws Exception {
        // Build and start the processor for the main queue
        this.mainQueueProcessorClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .queueName(queueName)
                .processMessage(context -> {
                    // Wrap your transactional method in a lambda for the processor
                    try {
                        processOrderInitiationMessage(context.getMessage());
                        context.complete(); // Acknowledge message if processing is successful
                    } catch (Exception e) {
                        System.err.println("Error processing message, abandoning: " + e.getMessage());
                        context.abandon(); // Requeue message if processing fails
                        // Consider dead-lettering explicitly for certain unrecoverable errors:
                        // context.deadLetter("Failed to process", e.getMessage());
                    }
                })
                .processError(processMainQueueError())
                .buildProcessorClient();

        // Build and start the processor for the dead-letter queue
        this.deadLetterQueueProcessorClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .queueName(queueName + "/$DeadLetterQueue") // Standard DLQ naming convention
                .processMessage(context -> {
                    try {
                        processDeadLetterMessage(context.getMessage());
                        context.complete(); // Acknowledge DLQ message if processing is successful
                    } catch (Exception e) {
                        System.err.println("Error processing dead-letter message, abandoning: " + e.getMessage());
                        context.abandon(); // Requeue DLQ message if processing fails
                    }
                })
                .processError(processDeadLetterQueueError())
                .buildProcessorClient();

        System.out.println("Starting Service Bus Processor Clients...");
        mainQueueProcessorClient.start();
        deadLetterQueueProcessorClient.start();
    }

    @Override
    public void destroy() throws Exception {
        // Stop and close the processor clients when the Spring context is destroyed
        if (mainQueueProcessorClient != null && mainQueueProcessorClient.isRunning()) {
            mainQueueProcessorClient.stop();
            mainQueueProcessorClient.close();
            System.out.println("Main ServiceBusProcessorClient stopped and closed.");
        }
        if (deadLetterQueueProcessorClient != null && deadLetterQueueProcessorClient.isRunning()) {
            deadLetterQueueProcessorClient.stop();
            deadLetterQueueProcessorClient.close();
            System.out.println("Dead Letter ServiceBusProcessorClient stopped and closed.");
        }
    }
}