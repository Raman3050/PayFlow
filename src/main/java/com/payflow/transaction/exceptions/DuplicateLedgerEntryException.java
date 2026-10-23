package com.payflow.transaction.exceptions;

public class DuplicateLedgerEntryException extends RuntimeException {
    public DuplicateLedgerEntryException(String message) {
        super(message);
    }
}