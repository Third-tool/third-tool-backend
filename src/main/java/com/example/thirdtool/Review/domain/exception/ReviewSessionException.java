package com.example.thirdtool.Review.domain.exception;

public class ReviewSessionException extends RuntimeException {

    public ReviewSessionException(String message) {
        super(message);
    }

    public ReviewSessionException(String message, Throwable cause) {
        super(message, cause);
    }
}

