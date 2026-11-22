package com.lims.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "publishers")
public class Publisher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "publisher_id")
    private Long id;

    @Column(name = "publisher_name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(length = 255)
    private String address;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
}
