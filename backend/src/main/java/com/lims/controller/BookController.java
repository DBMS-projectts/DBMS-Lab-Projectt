package com.lims.controller;

import com.lims.dto.BookRequest;
import com.lims.dto.BookResponse;
import com.lims.service.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class BookController {
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<BookResponse> getBooks(@RequestParam(required = false) String search,
                                       @RequestParam(required = false) String author,
                                       @RequestParam(required = false) String isbn,
                                       @RequestParam(required = false) Long categoryId,
                                       @RequestParam(required = false) Long publisherId,
                                       @RequestParam(required = false, defaultValue = "false") boolean availableOnly) {
        return bookService.findAll(search, author, isbn, categoryId, publisherId, availableOnly);
    }

    @GetMapping("/search")
    public Page<BookResponse> searchBooks(@RequestParam(required = false) String search,
                                          @RequestParam(required = false) String author,
                                          @RequestParam(required = false) String isbn,
                                          @RequestParam(required = false) Long categoryId,
                                          @RequestParam(required = false) Long publisherId,
                                          @RequestParam(required = false, defaultValue = "false") boolean availableOnly,
                                          Pageable pageable) {
        return bookService.search(search, author, isbn, categoryId, publisherId, availableOnly, pageable);
    }

    @GetMapping("/{id}")
    public BookResponse getBook(@PathVariable Long id) {
        return bookService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse createBook(@Valid @RequestBody BookRequest request) {
        return bookService.create(request);
    }

    @PutMapping("/{id}")
    public BookResponse updateBook(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return bookService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable Long id) {
        bookService.delete(id);
    }
}
