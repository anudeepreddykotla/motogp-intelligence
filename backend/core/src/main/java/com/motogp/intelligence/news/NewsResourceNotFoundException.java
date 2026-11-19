package com.motogp.intelligence.news;

import java.util.UUID;

public class NewsResourceNotFoundException extends RuntimeException {
    public NewsResourceNotFoundException(UUID articleId) {
        super("News article not found: " + articleId);
    }
}
