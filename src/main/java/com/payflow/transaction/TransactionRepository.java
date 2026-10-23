package com.payflow.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByFromWalletOrToWalletOrderByCreatedAtDesc(Long fromWallet, Long toWallet, Pageable pageable);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    Page<Transaction> findByFromWalletAndCreatedAtAfterOrToWalletAndCreatedAtAfterOrderByCreatedAtDesc(
        Long fromWallet, Instant after, Long toWallet, Instant after2, Pageable pageable);
    long countByFromWalletOrToWallet(Long fromWallet, Long toWallet);
    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);
}