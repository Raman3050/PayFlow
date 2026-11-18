package com.payflow.fraud;

import com.payflow.transaction.Transaction;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;

@Entity
@Table(name = "fraud_flags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many flags can point at one transaction (e.g. amount AND velocity rules both fire)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", referencedColumnName = "id")
    private Transaction transaction;

    @Column(name = "rule", nullable = false)
    private String rule;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "reviewed", nullable = false)
    @Builder.Default
    private boolean reviewed = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}