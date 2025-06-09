package com.example.springsoap.Model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer flightNumber;

    private String planeModel;
    private String source;
    private String destination;
    private Timestamp departureTime;
    private Timestamp arrivalTime;
    private BigDecimal priceEconomy;
    private BigDecimal priceBusiness;
    private BigDecimal priceFirst;

    @Transient
    private String duration;

    public Flight() {

    }

    public Flight(String flightString) {
        if (flightString == null || !flightString.startsWith("Flight{") || !flightString.endsWith("}")) {
            throw new IllegalArgumentException("Invalid Flight string format. Must start with 'Flight{' and end with '}'.");
        }
        String content = flightString.substring("Flight{".length(), flightString.length() - 1);
        java.util.Map<String, String> parsedFields = new java.util.HashMap<>();

        Pattern fieldPattern = Pattern.compile("(\\w+)=(?:'([^']*)'|([\\w\\d.\\-: ]+))");
        Matcher fieldMatcher = fieldPattern.matcher(content);

        while (fieldMatcher.find()) {
            String key = fieldMatcher.group(1);
            // Group 2 is for single-quoted strings, Group 3 is for non-quoted values
            String value = fieldMatcher.group(2) != null ? fieldMatcher.group(2) : fieldMatcher.group(3);
            parsedFields.put(key, value);
        }

        // Check if all expected fields are present to avoid NullPointerExceptions
        if (parsedFields.size() < 10) { // Expecting 10 fields based on toString()
            throw new IllegalArgumentException("Missing expected fields in Flight string: " + flightString);
        }

        try {
            this.flightNumber = Integer.valueOf(parsedFields.get("flightNumber"));
            this.planeModel = parsedFields.get("planeModel");
            this.source = parsedFields.get("source");
            this.destination = parsedFields.get("destination");
            this.departureTime = Timestamp.valueOf(parsedFields.get("departureTime"));
            this.arrivalTime = Timestamp.valueOf(parsedFields.get("arrivalTime"));
            this.priceEconomy = new BigDecimal(parsedFields.get("priceEconomy"));
            this.priceBusiness = new BigDecimal(parsedFields.get("priceBusiness"));
            this.priceFirst = new BigDecimal(parsedFields.get("priceFirst"));
            this.duration = parsedFields.get("duration");

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in flight string: " + flightString, e);
        } catch (IllegalArgumentException e) { // Catches issues from Timestamp.valueOf or BigDecimal
            throw new IllegalArgumentException("Failed to parse flight data from string: " + flightString + ". " + e.getMessage(), e);
        } catch (NullPointerException e) { // Catches if a key was somehow missing and .get() returned null
            throw new IllegalArgumentException("A required field was missing or incorrectly formatted in flight string: " + flightString, e);
        }
    }

    // Getters and Setters
    public Integer getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(Integer flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getPlaneModel() {
        return planeModel;
    }

    public void setPlaneModel(String planeModel) {
        this.planeModel = planeModel;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Timestamp getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(Timestamp departureTime) {
        this.departureTime = departureTime;
    }

    public Timestamp getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(Timestamp arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public BigDecimal getPriceEconomy() {
        return priceEconomy;
    }

    public void setPriceEconomy(BigDecimal priceEconomy) {
        this.priceEconomy = priceEconomy;
    }

    public BigDecimal getPriceBusiness() {
        return priceBusiness;
    }

    public void setPriceBusiness(BigDecimal priceBusiness) {
        this.priceBusiness = priceBusiness;
    }

    public BigDecimal getPriceFirst() {
        return priceFirst;
    }

    public void setPriceFirst(BigDecimal priceFirst) {
        this.priceFirst = priceFirst;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "Flight{" +
                "flightNumber=" + flightNumber +
                ", planeModel='" + planeModel + '\'' +
                ", source='" + source + '\'' +
                ", destination='" + destination + '\'' +
                ", departureTime=" + departureTime +
                ", arrivalTime=" + arrivalTime +
                ", priceEconomy=" + priceEconomy +
                ", priceBusiness=" + priceBusiness +
                ", priceFirst=" + priceFirst +
                ", duration='" + duration + '\'' +
                '}';
    }
}

