package com.example.DALN.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sports")
public class MonTheThao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    public MonTheThao() {
    }

    public MonTheThao(String name) {
        this.name = name;
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
}