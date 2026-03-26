package com.app.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "greeting")
public class Greeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
}
