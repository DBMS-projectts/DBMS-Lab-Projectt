package com.lims.service;

import com.lims.dto.BookRequest;
import com.lims.dto.BookResponse;
import com.lims.entity.Book;
import com.lims.entity.Category;
import com.lims.entity.Publisher;
import com.lims.repository.BookRepository;
import com.lims.repository.CategoryRepository;
import com.lims.repository.PublisherRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class BookService {
    private final BookRepository books;
    private final CategoryRepository categories;
    private final PublisherRepository publishers;

    public BookService(BookRepository books, CategoryRepository categories, PublisherRepository publishers) {
        this.books = books;
        this.categories = categories;
        this.publishers = publishers;
    }

    @Transactional(readOnly = true)
    public List<BookResponse> findAll(String search) {
        List<Book> result = search == null || search.isBlank()
            ? books.findAll()
            : books.findByTitleContainingIgnoreCaseOrIsbnContainingIgnoreCase(search.trim(), search.trim());
        return result.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BookResponse findById(Long id) {
        return toResponse(getBook(id));
    }

    public BookResponse create(BookRequest request) {
        if (books.existsByIsbn(request.isbn().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ISBN already exists");
        }
        Book book = new Book();
        applyRequest(book, request);
        return toResponse(books.save(book));
    }

    public BookResponse update(Long id, BookRequest request) {
        Book book = getBook(id);
        if (books.existsByIsbnAndIdNot(request.isbn().trim(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ISBN already exists");
        }
        applyRequest(book, request);
        return toResponse(books.save(book));
    }

    public void delete(Long id) {
        Book book = getBook(id);
        try {
            books.delete(book);
            books.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This book has related copies, reservations or borrowing records and cannot be deleted");
        }
    }

    private Book getBook(Long id) {
        return books.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
    }

    private void applyRequest(Book book, BookRequest request) {
        book.setIsbn(request.isbn().trim());
        book.setTitle(request.title().trim());
        book.setEdition(request.edition() == null ? null : request.edition().trim());
        book.setPublicationYear(request.publicationYear());
        Category category = categories.findById(request.categoryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        book.setCategory(category);
        Publisher publisher = request.publisherId() == null ? null : publishers.findById(request.publisherId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publisher not found"));
        book.setPublisher(publisher);
    }

    private BookResponse toResponse(Book book) {
        Category category = book.getCategory();
        Publisher publisher = book.getPublisher();
        return new BookResponse(
            book.getId(), book.getIsbn(), book.getTitle(), book.getEdition(), book.getPublicationYear(),
            category.getId(), category.getName(),
            publisher == null ? null : publisher.getId(),
            publisher == null ? null : publisher.getName()
        );
    }
}
