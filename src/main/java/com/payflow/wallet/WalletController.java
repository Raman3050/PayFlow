package com.payflow.wallet;

import com.payflow.security.UserPrincipal;
import com.payflow.transaction.TransactionService;
import com.payflow.wallet.dto.TopUpRequest;
import com.payflow.wallet.dto.WalletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final TransactionService transactionService;

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getMyWallet(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(walletService.getWallet(user.getId()));
    }

    /**
     * Adds money to the caller's own wallet. The wallet is taken from the JWT,
     * never from the request body. Returns the updated wallet with its new balance.
     */
    @PostMapping("/top-up")
    public ResponseEntity<WalletResponse> topUpWallet(
            @Valid @RequestBody TopUpRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        log.info("Topping up wallet for user {} with amount {}", user.getId(), request.getAmount());
        transactionService.topUp(user.getId(), request.getAmount(), request.getIdempotencyKey());
        return ResponseEntity.ok(walletService.getWallet(user.getId()));
    }
}
