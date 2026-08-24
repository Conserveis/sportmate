package com.sportmate.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Receipt")
public class Receipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PlaymentID")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;

    @Column(name = "DatePlayment", nullable = false)
    private LocalDateTime datePlayment = LocalDateTime.now();

    @Column(name = "PlaymentAmount", nullable = false)
    private BigDecimal amount;

    @Column(name = "QR")
    private String qr;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getDatePlayment() { return datePlayment; }
    public void setDatePlayment(LocalDateTime datePlayment) { this.datePlayment = datePlayment; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getQr() { return qr; }
    public void setQr(String qr) { this.qr = qr; }
}
