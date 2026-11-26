package com.lims.controller;

import com.lims.dto.AuthorRequest;
import com.lims.dto.AuthorResponse;
import com.lims.dto.BookSummaryResponse;
import com.lims.entity.Author;
import com.lims.repository.AuthorRepository;
import com.lims.repository.BookRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/authors")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class AuthorController {
    private final AuthorRepository authors;
    private final BookRepository books;

    public AuthorController(AuthorRepository authors, BookRepository books) {
        this.authors = authors;
        this.books = books;
    }

    @GetMapping
    public List<AuthorResponse> getAuthors(@RequestParam(required = false) String search) {
        List<Author> result = search == null || search.isBlank()
            ? authors.findAll()
            : authors.findByNameContainingIgnoreCase(search.trim());
        return result.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public AuthorResponse getAuthor(@PathVariable Long id) {
        return toResponse(findAuthor(id));
    }

    @GetMapping("/{id}/books")
    public List<BookSummaryResponse> getAuthorBooks(@PathVariable Long id) {
        findAuthor(id);
        return books.findDistinctByAuthors_Id(id).stream()
            .map(book -> new BookSummaryResponse(book.getId(), book.getTitle(), book.getIsbn()))
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorResponse createAuthor(@Valid @RequestBody AuthorRequest request) {
        Author author = new Author();
        applyRequest(author, request);
        return toResponse(authors.save(author));
    }

    @PutMapping("/{id}")
    public AuthorResponse updateAuthor(@PathVariable Long id, @Valid @RequestBody AuthorRequest request) {
        Author author = findAuthor(id);
        applyRequest(author, request);
        return toResponse(authors.save(author));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAuthor(@PathVariable Long id) {
        Author author = findAuthor(id);
        try {
            authors.delete(author);
            authors.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This author is linked to other records and cannot be deleted");
        }
    }

    private Author findAuthor(Long id) {
        return authors.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found"));
    }

    private void applyRequest(Author author, AuthorRequest request) {
        author.setName(request.name().trim());
        author.setCountry(request.country() == null ? null : request.country().trim());
    }

    private AuthorResponse toResponse(Author author) {
        return new AuthorResponse(author.getId(), author.getName(), author.getCountry());
    }
}
