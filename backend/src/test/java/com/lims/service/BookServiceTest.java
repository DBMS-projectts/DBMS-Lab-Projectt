package com.lims.service;

import com.lims.dto.BookRequest;
import com.lims.dto.BookResponse;
import com.lims.entity.Book;
import com.lims.entity.Category;
import com.lims.entity.Publisher;
import com.lims.repository.BookRepository;
import com.lims.repository.CategoryRepository;
import com.lims.repository.PublisherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {
    @Mock private BookRepository books;
    @Mock private CategoryRepository categories;
    @Mock private PublisherRepository publishers;

    @InjectMocks private BookService service;

    private Category category;
    private Publisher publisher;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Computer Science");

        publisher = new Publisher();
        publisher.setId(1L);
        publisher.setName("Sample Academic Press");
    }

    @Test
    void createBookMapsRelatedEntities() {
        BookRequest request = new BookRequest(
            "9781234567890", "Learning Java", "First Edition", 2025, 1L, 1L);

        when(books.existsByIsbn("9781234567890")).thenReturn(false);
        when(categories.findById(1L)).thenReturn(Optional.of(category));
        when(publishers.findById(1L)).thenReturn(Optional.of(publisher));
        when(books.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse response = service.create(request);

        assertEquals("Learning Java", response.title());
        assertEquals("Computer Science", response.categoryName());
        assertEquals("Sample Academic Press", response.publisherName());
        verify(books).save(any(Book.class));
    }

    @Test
    void createBookRejectsDuplicateIsbn() {
        BookRequest request = new BookRequest(
            "9781234567890", "Learning Java", null, 2025, 1L, null);
        when(books.existsByIsbn("9781234567890")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class, () -> service.create(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(books, never()).save(any(Book.class));
    }

    @Test
    void createBookRejectsUnknownCategory() {
        BookRequest request = new BookRequest(
            "9781234567890", "Learning Java", null, 2025, 99L, null);
        when(books.existsByIsbn("9781234567890")).thenReturn(false);
        when(categories.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class, () -> service.create(request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(books, never()).save(any(Book.class));
    }
}
