package com.example.DALN.model;

import jakarta.persistence.*;

@Entity
@Table(name = "courts")
public class SanBong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String type;

    private Double price;

    @ManyToOne
    @JoinColumn(name = "venue_id")
    private CoSoSan venue;

    public SanBong() {
    }

    public SanBong(String name, String type, Double price, CoSoSan venue) {
        this.name = name;
        this.type = type;
        this.price = price;
        this.venue = venue;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public CoSoSan getVenue() {
        return venue;
    }

    public void setVenue(CoSoSan venue) {
        this.venue = venue;
    }
}