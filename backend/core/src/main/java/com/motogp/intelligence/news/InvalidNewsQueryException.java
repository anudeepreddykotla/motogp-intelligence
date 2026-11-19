package com.motogp.intelligence.news;

public class InvalidNewsQueryException extends RuntimeException {
    public InvalidNewsQueryException(String message) {
        super(message);
    }
}
