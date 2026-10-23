package com.payflow.wallet.exceptions;

import com.payflow.exception.ResourceNotFoundException;

public class WalletNotFoundException extends ResourceNotFoundException {
    public WalletNotFoundException(String message) {
        super(message);
    }
}