package com.payflow.transaction;

import com.payflow.exception.IdempotencyConflictException;
import com.payflow.exception.InsufficientBalanceException;
import com.payflow.exception.InvalidTransferException;
import com.payflow.exception.ResourceNotFoundException;
import com.payflow.fraud.FraudDetectionService;
import com.payflow.fraud.FraudFlagRepository;
import com.payflow.transaction.dto.LedgerEntryResponse;
import com.payflow.transaction.dto.TransactionResponse;
import com.payflow.transaction.dto.TransferRequest;
import com.payflow.transaction.dto.TransferResponse;
import com.payflow.wallet.SystemAccounts;
import com.payflow.wallet.Wallet;
import com.payflow.wallet.WalletRepository;
import com.payflow.wallet.exceptions.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final LedgerEntryService ledgerEntryService;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionEventPublisher eventPublisher;
    private final FraudDetectionService fraudDetectionService;
    private final FraudFlagRepository fraudFlagRepository;
    private final SystemAccounts systemAccounts;

    /**
     * Moves money from the authenticated user's wallet to another wallet.
     * Runs in one database transaction: the PENDING record, both ledger rows and
     * the COMPLETED status either all commit or none do.
     */
    @Transactional
    public TransferResponse transfer(Long userId, TransferRequest request) {
        log.info("Processing transfer request: idempotency_key = {}", request.getIdempotencyKey());
        ensureNewIdempotencyKey(request.getIdempotencyKey());

        // Sender is always the caller's own wallet. Loading it with
        // OPTIMISTIC_FORCE_INCREMENT makes concurrent debits of the same wallet conflict.
        Wallet fromWallet = walletRepository.findByUserIdForDebit(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));

        if (request.getFromWalletId() != null && !request.getFromWalletId().equals(fromWallet.getId())) {
            throw new AccessDeniedException("You can only transfer from your own wallet");
        }

        Wallet toWallet = walletRepository.findById(request.getToWalletId())
                .filter(w -> !systemAccounts.isSystemWallet(w.getId()))
                .orElseThrow(() -> new WalletNotFoundException("Recipient wallet not found: " + request.getToWalletId()));

        if (toWallet.getId().equals(fromWallet.getId())) {
            throw new InvalidTransferException("You cannot transfer money to your own wallet");
        }

        String currency = request.getCurrency() == null ? fromWallet.getCurrency() : request.getCurrency();
        if (!currency.equals(fromWallet.getCurrency()) || !currency.equals(toWallet.getCurrency())) {
            throw new InvalidTransferException("Currency mismatch: both wallets must use " + currency);
        }

        ensureActive(fromWallet, "Your wallet");
        ensureActive(toWallet, "The recipient's wallet");

        BigDecimal balance = ledgerEntryRepository.balanceOf(fromWallet.getId());
        if (balance.compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance: available " + balance.setScale(2, RoundingMode.HALF_UP) + " " + currency);
        }

        Transaction transaction = postToLedger(request.getIdempotencyKey(), fromWallet, toWallet, request.getAmount(), currency);

        // Fraud rules run after the money moves and flag the transfer for human review.
        fraudDetectionService.checkFraudRules(transaction);

        return toTransferResponse(transaction);
    }

    /**
     * Adds money to a user's wallet. Recorded as a normal double-entry transfer
     * from the system funding wallet, so the user's balance stays ledger-derived.
     */
    @Transactional
    public TransferResponse topUp(Long userId, BigDecimal amount, String idempotencyKey) {
        String key = (idempotencyKey == null || idempotencyKey.isBlank())
                ? "topup-" + UUID.randomUUID()
                : idempotencyKey;
        ensureNewIdempotencyKey(key);

        Wallet toWallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
        ensureActive(toWallet, "Your wallet");

        Wallet systemWallet = walletRepository.findById(systemAccounts.systemWalletId())
                .orElseThrow(() -> new IllegalStateException("System wallet missing"));

        Transaction transaction = postToLedger(key, systemWallet, toWallet, amount, toWallet.getCurrency());
        return toTransferResponse(transaction);
    }

    private Transaction postToLedger(String key, Wallet from, Wallet to, BigDecimal amount, String currency) {
        Transaction transaction = Transaction.builder()
                .idempotencyKey(key)
                .fromWallet(from.getId())
                .toWallet(to.getId())
                .amount(amount)
                .currency(currency)
                .status(TransactionStatus.PENDING)
                .build();

        // Flush now so a concurrent request with the same idempotency key hits the
        // unique constraint here (mapped to HTTP 409) instead of at commit time.
        transaction = transactionRepository.saveAndFlush(transaction);

        ledgerEntryService.createDebitEntry(transaction, from);
        ledgerEntryService.createCreditEntry(transaction, to);

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction = transactionRepository.save(transaction);

        log.info("Transaction {} completed: {} {} from wallet {} to wallet {}",
                transaction.getId(), amount, currency, from.getId(), to.getId());

        // Published only after the database transaction commits.
        eventPublisher.publishTransactionCompleted(transaction);
        return transaction;
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long transactionId, Long userId, boolean isAdmin) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

        if (!isAdmin) {
            Long myWalletId = walletRepository.findByUserId(userId).map(Wallet::getId).orElse(null);
            boolean mine = transaction.getFromWallet().equals(myWalletId) || transaction.getToWallet().equals(myWalletId);
            if (!mine) {
                // 404, not 403, so users can't probe which transaction IDs exist
                throw new ResourceNotFoundException("Transaction not found: " + transactionId);
            }
        }

        TransactionResponse response = toTransactionResponse(
                transaction, fraudFlagRepository.existsByTransaction_IdAndReviewedFalse(transactionId));
        response.setLedgerEntries(ledgerEntryRepository.findByTransactionIdOrderByIdAsc(transactionId).stream()
                .map(e -> LedgerEntryResponse.builder()
                        .walletId(e.getWalletId())
                        .type(e.getType())
                        .amount(e.getAmount())
                        .currency(e.getCurrency())
                        .build())
                .toList());
        return response;
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(Long userId, Pageable pageable, Instant from, Instant to) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));

        Instant fromFilter = from != null ? from : Instant.EPOCH;
        Instant toFilter = to != null ? to : Instant.now().plus(1, ChronoUnit.DAYS);

        Page<Transaction> page = transactionRepository.findHistory(wallet.getId(), fromFilter, toFilter, pageable);

        List<Long> ids = page.getContent().stream().map(Transaction::getId).toList();
        Set<Long> flagged = ids.isEmpty()
                ? Set.of()
                : new HashSet<>(fraudFlagRepository.findOpenFlaggedTransactionIds(ids));

        return page.map(t -> toTransactionResponse(t, flagged.contains(t.getId())));
    }

    private void ensureNewIdempotencyKey(String key) {
        if (transactionRepository.findByIdempotencyKey(key).isPresent()) {
            log.info("Duplicate request detected (idempotency_key = {})", key);
            throw new IdempotencyConflictException("Duplicate transfer detected for idempotency key: " + key);
        }
    }

    private void ensureActive(Wallet wallet, String label) {
        if (wallet.getStatus() != Wallet.WalletStatus.ACTIVE) {
            throw new InvalidTransferException(label + " is " + wallet.getStatus().name().toLowerCase()
                    + " and cannot send or receive money");
        }
    }

    private TransferResponse toTransferResponse(Transaction transaction) {
        return TransferResponse.builder()
                .transactionId(transaction.getId())
                .idempotencyKey(transaction.getIdempotencyKey())
                .status(transaction.getStatus().name())
                .build();
    }

    private TransactionResponse toTransactionResponse(Transaction transaction, boolean underReview) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .idempotencyKey(transaction.getIdempotencyKey())
                .kind(systemAccounts.isSystemWallet(transaction.getFromWallet()) ? "TOP_UP" : "TRANSFER")
                .fromWallet(transaction.getFromWallet())
                .toWallet(transaction.getToWallet())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus().name())
                .failureReason(transaction.getFailureReason())
                .underReview(underReview)
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
