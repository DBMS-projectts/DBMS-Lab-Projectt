package com.lims.controller;

import com.lims.dto.CopyConditionRequest;
import com.lims.dto.CopyRequest;
import com.lims.dto.CopyResponse;
import com.lims.dto.CopyStatusRequest;
import com.lims.entity.Book;
import com.lims.entity.BookCopy;
import com.lims.repository.BookCopyRepository;
import com.lims.repository.BookRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/copies")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class BookCopyController {
    /** Statuses staff may set directly. BORROWED is only set by the issue procedure. */
    private static final Set<String> STAFF_STATUSES = Set.of("AVAILABLE", "DAMAGED", "LOST", "NOT_FOR_LOAN");
    private static final Set<String> CONDITIONS = Set.of("GOOD", "FAIR", "DAMAGED");

    private final BookCopyRepository copies;
    private final BookRepository books;

    public BookCopyController(BookCopyRepository copies, BookRepository books) {
        this.copies = copies;
        this.books = books;
    }

    @GetMapping
    public List<CopyResponse> getCopies(@RequestParam(required = false) Long bookId) {
        List<BookCopy> result = bookId == null ? copies.findAll() : copies.findByBookId(bookId);
        return result.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CopyResponse getCopy(@PathVariable Long id) {
        return toResponse(getCopy(id));
    }

    @GetMapping("/by-accession/{accessionNumber}")
    public CopyResponse getByAccession(@PathVariable String accessionNumber) {
        BookCopy copy = copies.findByAccessionNumber(accessionNumber)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Copy not found"));
        return toResponse(copy);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CopyResponse createCopy(@Valid @RequestBody CopyRequest request) {
        Book book = books.findById(request.bookId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
        String accession = request.accessionNumber().trim();
        if (copies.existsByAccessionNumber(accession)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Accession number already exists");
        }
        String condition = request.conditionStatus() == null
            ? "GOOD"
            : request.conditionStatus().trim().toUpperCase();
        if (!CONDITIONS.contains(condition)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown condition: " + condition);
        }
        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setAccessionNumber(accession);
        copy.setCopyStatus("AVAILABLE");
        copy.setConditionStatus(condition);
        return toResponse(copies.save(copy));
    }

    @PutMapping("/{id}/status")
    public CopyResponse updateStatus(@PathVariable Long id, @Valid @RequestBody CopyStatusRequest request) {
        String status = request.status().trim().toUpperCase();
        if ("BORROWED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "BORROWED status is set by the issue flow and cannot be assigned manually");
        }
        if (!STAFF_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown copy status: " + status);
        }
        BookCopy copy = getCopy(id);
        copy.setCopyStatus(status);
        return toResponse(copies.save(copy));
    }

    @PutMapping("/{id}/condition")
    public CopyResponse updateCondition(@PathVariable Long id,
                                        @Valid @RequestBody CopyConditionRequest request) {
        String condition = request.conditionStatus().trim().toUpperCase();
        if (!CONDITIONS.contains(condition)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown condition: " + condition);
        }
        BookCopy copy = getCopy(id);
        copy.setConditionStatus(condition);
        return toResponse(copies.save(copy));
    }

    private BookCopy getCopy(Long id) {
        return copies.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Copy not found"));
    }

    private CopyResponse toResponse(BookCopy copy) {
        return new CopyResponse(
            copy.getId(),
            copy.getBook().getId(),
            copy.getBook().getTitle(),
            copy.getAccessionNumber(),
            copy.getCopyStatus(),
            copy.getConditionStatus()
        );
    }
}
