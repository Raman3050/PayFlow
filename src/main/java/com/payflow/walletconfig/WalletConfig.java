package com.payflow.walletconfig;

import com.payflow.wallet.Wallet;
import com.payflow.wallet.WalletRepository;
import com.payflow.wallet.dto.TopUpRequest;
import com.payflow.wallet.dto.WalletResponse;
import com.payflow.wallet.exceptions.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/wallets")
@RequiredArgsConstructor
public class WalletConfig {

    private final WalletRepository walletRepository;

    @GetMapping("/{walletId}")
    @PreAuthorize("hasRole('ADMIN')")
    public WalletResponse getWalletById(@PathVariable Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus().name())
                .version(wallet.getVersion())
                .createdAt(wallet.getCreatedAt())
                .build();
    }

    @PostMapping("/{walletId}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    public WalletResponse freezeWallet(@PathVariable Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        wallet.setStatus(Wallet.WalletStatus.FROZEN);
        wallet.setVersion(wallet.getVersion() + 1);

        wallet = walletRepository.save(wallet);
        log.info("Frozen wallet {} for user {}", wallet.getId(), wallet.getUserId());

        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus().name())
                .version(wallet.getVersion())
                .createdAt(wallet.getCreatedAt())
                .build();
    }

    @PostMapping("/{walletId}/unfreeze")
    @PreAuthorize("hasRole('ADMIN')")
    public WalletResponse unfreezeWallet(@PathVariable Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        wallet.setStatus(Wallet.WalletStatus.ACTIVE);
        wallet.setVersion(wallet.getVersion() + 1);

        wallet = walletRepository.save(wallet);
        log.info("Unfrozen wallet {} for user {}", wallet.getId(), wallet.getUserId());

        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus().name())
                .version(wallet.getVersion())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
}