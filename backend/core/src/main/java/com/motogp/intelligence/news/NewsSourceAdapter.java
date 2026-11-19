package com.motogp.intelligence.news;

import java.util.List;

public interface NewsSourceAdapter {
    String sourceType();

    List<NewsArticleRecord> fetch(NewsSourceDefinition source);
}
