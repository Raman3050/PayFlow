package com.payflow.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    Optional<LedgerEntry> findByTransactionIdAndWalletIdAndType(Long transactionId, Long walletId, String type);
    boolean existsByTransactionIdAndWalletIdAndType(Long transactionId, Long walletId, String type);

    List<LedgerEntry> findByTransactionIdOrderByIdAsc(Long transactionId);

    /**
     * A wallet's balance is never stored: it is always derived from the ledger
     * as SUM(credits) - SUM(debits).
     */
    @Query(value = """
            SELECT COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END), 0)
            FROM ledger_entries
            WHERE wallet_id = :walletId
            """, nativeQuery = true)
    BigDecimal balanceOf(@Param("walletId") Long walletId);
}
