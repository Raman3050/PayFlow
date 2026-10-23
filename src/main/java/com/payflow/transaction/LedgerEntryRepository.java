package com.payflow.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    Optional<LedgerEntry> findByTransactionIdAndWalletIdAndType(Long transactionId, Long walletId, String type);
    boolean existsByTransactionIdAndWalletIdAndType(Long transactionId, Long walletId, String type);
}