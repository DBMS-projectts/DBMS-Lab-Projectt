package com.lims.repository;

import com.lims.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByTitleContainingIgnoreCaseOrIsbnContainingIgnoreCase(String title, String isbn);
    boolean existsByIsbn(String isbn);
    boolean existsByIsbnAndIdNot(String isbn, Long id);
    List<Book> findDistinctByAuthors_Id(Long authorId);

    @Query("""
        SELECT DISTINCT b FROM Book b
        LEFT JOIN b.authors a
        WHERE (:search IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:author IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :author, '%')))
          AND (:isbn IS NULL OR b.isbn = :isbn)
          AND (:categoryId IS NULL OR b.category.id = :categoryId)
          AND (:publisherId IS NULL OR b.publisher.id = :publisherId)
        """)
    List<Book> searchBooks(@Param("search") String search,
                           @Param("author") String author,
                           @Param("isbn") String isbn,
                           @Param("categoryId") Long categoryId,
                           @Param("publisherId") Long publisherId);

    @Query("""
        SELECT DISTINCT b FROM Book b
        LEFT JOIN b.authors a
        WHERE (:search IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:author IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :author, '%')))
          AND (:isbn IS NULL OR b.isbn = :isbn)
          AND (:categoryId IS NULL OR b.category.id = :categoryId)
          AND (:publisherId IS NULL OR b.publisher.id = :publisherId)
        """)
    Page<Book> searchBooks(@Param("search") String search,
                           @Param("author") String author,
                           @Param("isbn") String isbn,
                           @Param("categoryId") Long categoryId,
                           @Param("publisherId") Long publisherId,
                           Pageable pageable);

    @Query("""
        SELECT DISTINCT b FROM Book b
        LEFT JOIN b.authors a
        WHERE (:search IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:author IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :author, '%')))
          AND (:isbn IS NULL OR b.isbn = :isbn)
          AND (:categoryId IS NULL OR b.category.id = :categoryId)
          AND (:publisherId IS NULL OR b.publisher.id = :publisherId)
          AND EXISTS (SELECT c FROM BookCopy c WHERE c.book = b AND c.copyStatus = 'AVAILABLE')
        """)
    List<Book> searchAvailableBooks(@Param("search") String search,
                                    @Param("author") String author,
                                    @Param("isbn") String isbn,
                                    @Param("categoryId") Long categoryId,
                                    @Param("publisherId") Long publisherId);

    @Query("""
        SELECT DISTINCT b FROM Book b
        LEFT JOIN b.authors a
        WHERE (:search IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:author IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :author, '%')))
          AND (:isbn IS NULL OR b.isbn = :isbn)
          AND (:categoryId IS NULL OR b.category.id = :categoryId)
          AND (:publisherId IS NULL OR b.publisher.id = :publisherId)
          AND EXISTS (SELECT c FROM BookCopy c WHERE c.book = b AND c.copyStatus = 'AVAILABLE')
        """)
    Page<Book> searchAvailableBooks(@Param("search") String search,
                                    @Param("author") String author,
                                    @Param("isbn") String isbn,
                                    @Param("categoryId") Long categoryId,
                                    @Param("publisherId") Long publisherId,
                                    Pageable pageable);
}
