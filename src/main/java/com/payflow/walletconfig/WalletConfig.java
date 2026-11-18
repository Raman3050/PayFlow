package com.payflow.walletconfig;

import com.payflow.wallet.Wallet;
import com.payflow.wallet.WalletService;
import com.payflow.wallet.dto.WalletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Admin wallet operations: inspect, freeze and unfreeze any wallet. */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/wallets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class WalletConfig {

    private final WalletService walletService;

    @GetMapping("/{walletId}")
    public WalletResponse getWalletById(@PathVariable Long walletId) {
        return walletService.getWalletById(walletId);
    }

    @PostMapping("/{walletId}/freeze")
    public WalletResponse freezeWallet(@PathVariable Long walletId) {
        return walletService.setStatus(walletId, Wallet.WalletStatus.FROZEN);
    }

    @PostMapping("/{walletId}/unfreeze")
    public WalletResponse unfreezeWallet(@PathVariable Long walletId) {
        return walletService.setStatus(walletId, Wallet.WalletStatus.ACTIVE);
    }
}
