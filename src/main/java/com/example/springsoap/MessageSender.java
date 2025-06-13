package com.example.springsoap;

import org.springframework.stereotype.Service;
import com.example.springsoap.Model.OrderProcessingMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import com.fasterxml.jackson.databind.ObjectMapper; // For JSON serialization

@Service
public class MessageSender {

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper; // To convert objects to JSON string
    private final String orderInitiationQueueName; // Name of your Service Bus Queue

    public MessageSender(JmsTemplate jmsTemplate,
                         ObjectMapper objectMapper,
                         @Value("${azure.servicebus.queue-name}") String orderInitiationQueueName) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
        this.orderInitiationQueueName = orderInitiationQueueName;
    }

    public void sendOrderInitiation(OrderProcessingMessage message) {
        try {
            // Convert the message object to a JSON string
            String jsonMessage = objectMapper.writeValueAsString(message);
            // Send the JSON string to the Service Bus Queue
            jmsTemplate.convertAndSend(orderInitiationQueueName, jsonMessage);
            System.out.println("Sent order initiation message for Order ID: " + message.getOrderId() + " to Service Bus Queue: " + orderInitiationQueueName);
        } catch (Exception e) {
            System.err.println("Error sending message to Service Bus: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send message to Service Bus", e);
        }
    }
}