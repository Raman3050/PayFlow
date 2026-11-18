package com.payflow.transaction;

import com.payflow.transaction.dto.CreateLedgerEntryRequest;
import com.payflow.transaction.exceptions.DuplicateLedgerEntryException;
import com.payflow.wallet.Wallet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerEntryService {

    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional
    public void createDebitEntry(Transaction transaction, Wallet wallet) {
        validateAndCreateEntry(transaction, wallet, "DEBIT");
    }

    @Transactional
    public void createCreditEntry(Transaction transaction, Wallet wallet) {
        validateAndCreateEntry(transaction, wallet, "CREDIT");
    }

    private void validateAndCreateEntry(Transaction transaction, Wallet wallet, String type) {
        log.info("Creating {} entry for transaction {} and wallet {}", type, transaction.getId(), wallet.getId());

        // Check for duplicate entry (idempotency on database level)
        ledgerEntryRepository.findByTransactionIdAndWalletIdAndType(
                transaction.getId(), wallet.getId(), type
        ).ifPresent(entry -> {
            log.error("Duplicate ledger entry detected: transaction {}, wallet {}, type {}",
                    transaction.getId(), wallet.getId(), type);
            throw new DuplicateLedgerEntryException(
                    "Duplicate ledger entry for transaction " + transaction.getId() + " and wallet " + wallet.getId()
            );
        });

        // Create the ledger entry
        LedgerEntry entry = LedgerEntry.builder()
                .transactionId(transaction.getId())
                .walletId(wallet.getId())
                .type(type)
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .build();

        ledgerEntryRepository.save(entry);
        log.info("Created {} ledger entry with ID {} for transaction {}", type, entry.getId(), transaction.getId());
    }
}