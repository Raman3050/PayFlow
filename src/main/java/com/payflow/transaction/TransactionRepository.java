package com.payflow.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    /** All transactions a wallet sent or received inside [from, to], newest first. */
    @Query(value = """
            SELECT t FROM Transaction t
            WHERE (t.fromWallet = :walletId OR t.toWallet = :walletId)
              AND t.createdAt >= :from AND t.createdAt <= :to
            ORDER BY t.createdAt DESC, t.id DESC
            """,
            countQuery = """
            SELECT COUNT(t) FROM Transaction t
            WHERE (t.fromWallet = :walletId OR t.toWallet = :walletId)
              AND t.createdAt >= :from AND t.createdAt <= :to
            """)
    Page<Transaction> findHistory(@Param("walletId") Long walletId,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to,
                                  Pageable pageable);

    /** Used by the velocity fraud rule: transfers sent by a wallet since a point in time. */
    long countByFromWalletAndCreatedAtAfter(Long fromWallet, Instant after);
}
