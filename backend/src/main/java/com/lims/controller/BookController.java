package com.lims.controller;

import com.lims.entity.Book;
import com.lims.repository.BookRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = "*")
public class BookController {
    private final BookRepository books;

    public BookController(BookRepository books) {
        this.books = books;
    }

    @GetMapping
    public List<Book> getBooks(@RequestParam(required = false) String search) {
        if (search == null || search.isBlank()) return books.findAll();
        return books.findByTitleContainingIgnoreCaseOrIsbnContainingIgnoreCase(search, search);
    }

    @GetMapping("/{id}")
    public Book getBook(@PathVariable Long id) {
        return books.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
    }
}