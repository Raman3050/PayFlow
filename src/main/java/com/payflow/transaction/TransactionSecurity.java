package com.payflow.transaction;

import com.payflow.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component("transactionSecurity")
@RequiredArgsConstructor
public class TransactionSecurity {

    private final WalletService walletService;

    public boolean canTransfer(Long walletId) {
        if (walletId == null) return false;

        try {
            Long userId = walletId; // Simplification: assuming walletId equals userId
            return walletService.existsByUserId(userId);
        } catch (Exception e) {
            return false;
        }
    }
}