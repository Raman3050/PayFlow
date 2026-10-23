package com.payflow.transaction;

import com.payflow.transaction.dto.TransferRequest;
import com.payflow.transaction.dto.TransferResponse;
import com.payflow.transaction.dto.TransactionResponse;
import com.payflow.wallet.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

import com.payflow.security.UserPrincipal;
import com.payflow.user.User;
import com.payflow.user.UserRepository;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final WalletService walletService;
    private final UserRepository userRepository;

    private Long getUserId(UserDetails userDetails) {
        if (userDetails instanceof UserPrincipal up) {
            return up.getId();
        }
        return userRepository.findByEmail(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
    }

    @PostMapping("/transfer")
    @PreAuthorize("@transactionSecurity.canTransfer(#request.fromWalletId)")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Processing transfer from wallet {} to wallet {} with amount {}",
                request.getFromWalletId(), request.getToWalletId(), request.getAmount());

        TransferResponse response = transactionService.transfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/history")
    public ResponseEntity<Page<TransactionResponse>> getTransactionHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        log.info("Fetching transaction history for user: {}", userDetails.getUsername());
        Long userId = getUserId(userDetails);

        Page<TransactionResponse> history = transactionService.getTransactionHistory(userId, pageable, from, to);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Fetching transaction {} for user: {}", transactionId, userDetails.getUsername());

        TransactionResponse transaction = transactionService.getTransaction(transactionId);
        return ResponseEntity.ok(transaction);
    }
}