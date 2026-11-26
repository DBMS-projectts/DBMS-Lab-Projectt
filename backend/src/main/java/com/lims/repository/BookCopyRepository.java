package com.lims.repository;

import com.lims.entity.BookCopy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {
    List<BookCopy> findByBookId(Long bookId);
    Optional<BookCopy> findByAccessionNumber(String accessionNumber);
    boolean existsByAccessionNumber(String accessionNumber);
    long countByBookIdAndCopyStatus(Long bookId, String copyStatus);
}
