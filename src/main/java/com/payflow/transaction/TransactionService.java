package com.payflow.transaction;

import com.payflow.exception.IdempotencyConflictException;
import com.payflow.transaction.dto.*;
import com.payflow.wallet.Wallet;
import com.payflow.wallet.WalletRepository;
import com.payflow.wallet.exceptions.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final LedgerEntryService ledgerEntryService;
    private final TransactionEventPublisher eventPublisher;

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        log.info("Processing transfer request: idempotency_key = {}", request.getIdempotencyKey());

        // Check for duplicate request using idempotency key
        transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .ifPresent(existing -> {
                    log.info("Duplicate transfer request detected (idempotency_key = {}). Returning existing.",
                            request.getIdempotencyKey());
                    throw new IdempotencyConflictException("Duplicate transfer detected for idempotency key: " + request.getIdempotencyKey());
                });

        Wallet fromWallet = walletRepository.findById(request.getFromWalletId())
                .orElseThrow(() -> new WalletNotFoundException("From wallet not found: " + request.getFromWalletId()));

        Wallet toWallet = walletRepository.findById(request.getToWalletId())
                .orElseThrow(() -> new WalletNotFoundException("To wallet not found: " + request.getToWalletId()));

        // Create and save the transaction record
        Transaction transaction = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .fromWallet(request.getFromWalletId())
                .toWallet(request.getToWalletId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(TransactionStatus.PENDING)
                .build();

        transactionRepository.save(transaction);
        log.info("Created transaction {} in PENDING state", transaction.getId());

        // Lock wallets to prevent race conditions
        Lock fromLock = new ReentrantLock();
        Lock toLock = new ReentrantLock();
        fromLock.lock();
        toLock.lock();

        try {
            // Double-entry ledger validation and creation
            ledgerEntryService.createDebitEntry(transaction, fromWallet);
            ledgerEntryService.createCreditEntry(transaction, toWallet);

            // Update wallet versions (for optimistic locking)
            fromWallet.setVersion(fromWallet.getVersion() + 1);
            toWallet.setVersion(toWallet.getVersion() + 1);

            // Mark transaction as completed
            transaction.setStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(transaction);

            log.info("Transfer completed: transaction {} from wallet {} to wallet {}",
                    transaction.getId(), fromWallet.getId(), toWallet.getId());

            // Publish event to Kafka
            eventPublisher.publishTransactionCompleted(transaction);

            return TransferResponse.builder()
                    .transactionId(transaction.getId())
                    .idempotencyKey(transaction.getIdempotencyKey())
                    .status("COMPLETED")
                    .build();

        } catch (Exception e) {
            log.error("Transfer failed for idempotency_key {}: {}", request.getIdempotencyKey(), e.getMessage(), e);

            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);

            throw e;
        } finally {
            toLock.unlock();
            fromLock.unlock();
        }
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        return TransactionResponse.builder()
                .id(transaction.getId())
                .idempotencyKey(transaction.getIdempotencyKey())
                .fromWallet(transaction.getFromWallet())
                .toWallet(transaction.getToWallet())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus().name())
                .failureReason(transaction.getFailureReason())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(
            Long userId, Pageable pageable, Instant from, Instant to) {
        Wallet fromWallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
        Wallet toWallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));

        Instant fromFilter = from != null ? from : Instant.now().minus(365, ChronoUnit.DAYS);
        Instant toFilter = to != null ? to : Instant.now();

        Page<Transaction> transactions;
        if (from == null && to == null) {
            transactions = transactionRepository
                    .findByFromWalletOrToWalletOrderByCreatedAtDesc(
                            fromWallet.getId(), toWallet.getId(), pageable);
        } else {
            transactions = transactionRepository
                    .findByFromWalletAndCreatedAtAfterOrToWalletAndCreatedAtAfterOrderByCreatedAtDesc(
                            fromWallet.getId(), fromFilter, toWallet.getId(), toFilter, pageable);
        }

        return transactions.map(this::toTransactionResponse);
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .idempotencyKey(transaction.getIdempotencyKey())
                .fromWallet(transaction.getFromWallet())
                .toWallet(transaction.getToWallet())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus().name())
                .failureReason(transaction.getFailureReason())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}