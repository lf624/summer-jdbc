package com.learn.summer.exception;

public class TransactionException extends DataAccessException{
    public TransactionException(String message) {
        super(message);
    }

    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransactionException(Throwable cause) {
        super(cause);
    }

    public TransactionException() {
        super();
    }
}
