package com.payflow.transaction;

import com.payflow.security.UserPrincipal;
import com.payflow.transaction.dto.TransactionResponse;
import com.payflow.transaction.dto.TransferRequest;
import com.payflow.transaction.dto.TransferResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /** Sends money from the caller's own wallet. */
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        log.info("User {} transferring {} to wallet {}", user.getId(), request.getAmount(), request.getToWalletId());
        TransferResponse response = transactionService.transfer(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** The caller's transactions, newest first. Optional ISO-8601 from/to filters. */
    @GetMapping("/history")
    public ResponseEntity<Page<TransactionResponse>> getTransactionHistory(
            @AuthenticationPrincipal UserPrincipal user,
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(transactionService.getTransactionHistory(user.getId(), pageable, from, to));
    }

    /** One transaction with its ledger entries. Users only see their own; admins see all. */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal UserPrincipal user) {
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return ResponseEntity.ok(transactionService.getTransaction(transactionId, user.getId(), isAdmin));
    }
}
