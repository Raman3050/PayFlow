package com.payflow.exception;

/** A transfer or top-up that breaks a business rule (frozen wallet, same wallet, currency mismatch...). */
public class InvalidTransferException extends RuntimeException {
    public InvalidTransferException(String message) { super(message); }
}
