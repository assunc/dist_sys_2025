package com.example.springsoap;

import com.example.springsoap.Model.OrderProcessingMessage;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.DisposableBean; // Import DisposableBean
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MessageSender implements DisposableBean { // Implement DisposableBean

    private final ServiceBusSenderClient senderClient;
    private final ObjectMapper objectMapper;

    public MessageSender(@Value("${azure.servicebus.connection-string}") String connectionString,
                         @Value("${azure.servicebus.queue-name}") String queueName,
                         ObjectMapper objectMapper) {
        // Build the ServiceBusSenderClient
        this.senderClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildClient();
        this.objectMapper = objectMapper;
    }

    public void sendOrderInitiation(OrderProcessingMessage message) {
        try {
            // Convert the message object to a JSON string
            String jsonMessage = objectMapper.writeValueAsString(message);
            System.out.println(jsonMessage);
            // Send the JSON string to the Service Bus Queue
            ServiceBusMessage serviceBusMessage = new ServiceBusMessage(jsonMessage);
            senderClient.sendMessage(serviceBusMessage); // Send the message
            System.out.println("Sent order initiation message for Order ID: " + message.getOrderId() + " to Service Bus Queue");
        } catch (Exception e) {
            System.err.println("Error sending message to Service Bus: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send message to Service Bus", e);
        }
    }

    @Override
    public void destroy() throws Exception {
        // Close the sender client when the Spring context is destroyed
        if (senderClient != null) {
            senderClient.close();
            System.out.println("ServiceBusSenderClient closed.");
        }
    }
}