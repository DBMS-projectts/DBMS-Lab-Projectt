package com.lims.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "members")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_type_id", nullable = false)
    private MemberType memberType;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "membership_status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public Long getId() { return id; }
    public User getUser() { return user; }
    public MemberType getMemberType() { return memberType; }
    public LocalDate getJoinDate() { return joinDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public String getStatus() { return status; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setMemberType(MemberType memberType) { this.memberType = memberType; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public void setStatus(String status) { this.status = status; }
}
