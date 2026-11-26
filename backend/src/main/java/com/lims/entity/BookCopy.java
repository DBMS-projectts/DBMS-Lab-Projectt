package com.lims.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "book_copies")
public class BookCopy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "copy_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "accession_number", nullable = false, unique = true, length = 50)
    private String accessionNumber;

    @Column(name = "copy_status", nullable = false, length = 20)
    private String copyStatus = "AVAILABLE";

    @Column(name = "condition_status", nullable = false, length = 20)
    private String conditionStatus = "GOOD";

    public Long getId() { return id; }
    public Book getBook() { return book; }
    public String getAccessionNumber() { return accessionNumber; }
    public String getCopyStatus() { return copyStatus; }
    public String getConditionStatus() { return conditionStatus; }

    public void setId(Long id) { this.id = id; }
    public void setBook(Book book) { this.book = book; }
    public void setAccessionNumber(String accessionNumber) { this.accessionNumber = accessionNumber; }
    public void setCopyStatus(String copyStatus) { this.copyStatus = copyStatus; }
    public void setConditionStatus(String conditionStatus) { this.conditionStatus = conditionStatus; }
}
