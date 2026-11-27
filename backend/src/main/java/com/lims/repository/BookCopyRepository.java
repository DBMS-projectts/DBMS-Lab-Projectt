package com.lims.repository;

import com.lims.entity.BookCopy;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    @Override
    @EntityGraph(attributePaths = "book")
    List<BookCopy> findAll();

    @Override
    @EntityGraph(attributePaths = "book")
    Optional<BookCopy> findById(Long id);

    @EntityGraph(attributePaths = "book")
    List<BookCopy> findByBookId(Long bookId);

    @EntityGraph(attributePaths = "book")
    Optional<BookCopy> findByAccessionNumber(String accessionNumber);

    boolean existsByAccessionNumber(String accessionNumber);

    long countByBookIdAndCopyStatus(Long bookId, String copyStatus);
}
