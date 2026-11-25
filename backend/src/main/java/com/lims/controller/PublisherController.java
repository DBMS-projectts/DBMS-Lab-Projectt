package com.lims.controller;

import com.lims.entity.Publisher;
import com.lims.repository.PublisherRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/publishers")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class PublisherController {
    private final PublisherRepository publishers;

    public PublisherController(PublisherRepository publishers) {
        this.publishers = publishers;
    }

    @GetMapping
    public List<Publisher> getPublishers() {
        return publishers.findAll();
    }
}
