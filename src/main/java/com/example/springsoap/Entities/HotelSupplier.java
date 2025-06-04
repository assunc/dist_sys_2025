package com.example.springsoap.Entities;

import jakarta.persistence.*;

@Entity
@Table(name = "hotelsupplier")
public class HotelSupplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "url", length = 250)
    private String url;

    // Constructors
    public HotelSupplier() {
    }

    public HotelSupplier(String name, String url) {
        this.name = name;
        this.url = url;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "HotelSupplier{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}