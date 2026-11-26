package com.lims.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "member_types")
public class MemberType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_type_id")
    private Long id;

    @Column(name = "type_name", nullable = false, unique = true, length = 50)
    private String typeName;

    @Column(name = "max_books", nullable = false)
    private int maxBooks;

    @Column(name = "borrowing_days", nullable = false)
    private int borrowingDays;

    @Column(name = "borrowing_months", nullable = false)
    private int borrowingMonths;

    @Column(name = "fine_per_day", nullable = false, precision = 8, scale = 2)
    private BigDecimal finePerDay;

    public Long getId() { return id; }
    public String getTypeName() { return typeName; }
    public int getMaxBooks() { return maxBooks; }
    public int getBorrowingDays() { return borrowingDays; }
    public int getBorrowingMonths() { return borrowingMonths; }
    public BigDecimal getFinePerDay() { return finePerDay; }

    public void setId(Long id) { this.id = id; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public void setMaxBooks(int maxBooks) { this.maxBooks = maxBooks; }
    public void setBorrowingDays(int borrowingDays) { this.borrowingDays = borrowingDays; }
    public void setBorrowingMonths(int borrowingMonths) { this.borrowingMonths = borrowingMonths; }
    public void setFinePerDay(BigDecimal finePerDay) { this.finePerDay = finePerDay; }
}
