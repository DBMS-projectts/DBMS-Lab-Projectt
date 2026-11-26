package com.lims.service;

import com.lims.dto.BookRequest;
import com.lims.dto.BookResponse;
import com.lims.entity.Author;
import com.lims.entity.Book;
import com.lims.entity.Category;
import com.lims.entity.Publisher;
import com.lims.repository.AuthorRepository;
import com.lims.repository.BookRepository;
import com.lims.repository.CategoryRepository;
import com.lims.repository.PublisherRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class BookService {
    private final BookRepository books;
    private final CategoryRepository categories;
    private final PublisherRepository publishers;
    private final AuthorRepository authors;

    public BookService(BookRepository books, CategoryRepository categories,
                       PublisherRepository publishers, AuthorRepository authors) {
        this.books = books;
        this.categories = categories;
        this.publishers = publishers;
        this.authors = authors;
    }

    @Transactional(readOnly = true)
    public List<BookResponse> findAll(String search) {
        return findAll(search, null, null, null, null, false);
    }

    @Transactional(readOnly = true)
    public List<BookResponse> findAll(String search, String author, String isbn,
                                      Long categoryId, Long publisherId, boolean availableOnly) {
        String searchText = blankToNull(search);
        String authorText = blankToNull(author);
        String isbnText = blankToNull(isbn);
        List<Book> result;
        if (authorText == null && isbnText == null && categoryId == null && publisherId == null && !availableOnly) {
            // Original catalogue behaviour: plain list or title/ISBN search.
            result = searchText == null
                ? books.findAll()
                : books.findByTitleContainingIgnoreCaseOrIsbnContainingIgnoreCase(searchText, searchText);
        } else if (availableOnly) {
            result = books.searchAvailableBooks(searchText, authorText, isbnText, categoryId, publisherId);
        } else {
            result = books.searchBooks(searchText, authorText, isbnText, categoryId, publisherId);
        }
        return result.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<BookResponse> search(String search, String author, String isbn,
                                     Long categoryId, Long publisherId, boolean availableOnly,
                                     Pageable pageable) {
        String searchText = blankToNull(search);
        String authorText = blankToNull(author);
        String isbnText = blankToNull(isbn);
        Page<Book> page = availableOnly
            ? books.searchAvailableBooks(searchText, authorText, isbnText, categoryId, publisherId, pageable)
            : books.searchBooks(searchText, authorText, isbnText, categoryId, publisherId, pageable);
        return page.map(this::toResponse);
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
        book.setCoverImageUrl(request.coverImageUrl() == null ? null : request.coverImageUrl().trim());
        Category category = categories.findById(request.categoryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        book.setCategory(category);
        Publisher publisher = request.publisherId() == null ? null : publishers.findById(request.publisherId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publisher not found"));
        book.setPublisher(publisher);
        if (request.authorIds() != null && authors != null) {
            Set<Author> resolved = new LinkedHashSet<>();
            for (Long authorId : request.authorIds()) {
                Author author = authors.findById(authorId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Author not found: " + authorId));
                resolved.add(author);
            }
            book.setAuthors(resolved);
        }
    }

    private BookResponse toResponse(Book book) {
        Category category = book.getCategory();
        Publisher publisher = book.getPublisher();
        List<BookResponse.AuthorSummary> authorSummaries = book.getAuthors() == null
            ? List.of()
            : book.getAuthors().stream()
                .map(author -> new BookResponse.AuthorSummary(author.getId(), author.getName()))
                .sorted(Comparator.comparing(BookResponse.AuthorSummary::name))
                .toList();
        return new BookResponse(
            book.getId(), book.getIsbn(), book.getTitle(), book.getEdition(), book.getPublicationYear(),
            category.getId(), category.getName(),
            publisher == null ? null : publisher.getId(),
            publisher == null ? null : publisher.getName(),
            authorSummaries,
            book.getCoverImageUrl()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
