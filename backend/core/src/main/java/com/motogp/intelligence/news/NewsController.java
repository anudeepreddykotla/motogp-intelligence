package com.motogp.intelligence.news;

import com.motogp.intelligence.common.ApiResponse;
import com.motogp.intelligence.common.PaginatedResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/news")
@Validated
public class NewsController {
    private static final int MAX_LIMIT = 100;
    private final NewsReadStore store;

    public NewsController(NewsReadStore store) {
        this.store = store;
    }

    @GetMapping
    public ApiResponse<PaginatedResponse<NewsReadStore.NewsArticleView>> articles(
            @RequestParam(required = false) @Size(max = 200) String title,
            @RequestParam(required = false) @Size(max = 120) String publisher,
            @RequestParam(required = false) Instant publishedFrom,
            @RequestParam(required = false) Instant publishedTo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_LIMIT) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset) {
        if (publishedFrom != null && publishedTo != null && publishedFrom.isAfter(publishedTo)) {
            throw new InvalidNewsQueryException("publishedFrom must not be after publishedTo");
        }
        return paginated(store.articles(blankToNull(title), blankToNull(publisher), publishedFrom, publishedTo, limit, offset), limit, offset);
    }

    @GetMapping("/publishers")
    public ApiResponse<List<String>> publishers() {
        return ApiResponse.success(requestId(), store.publishers());
    }

    @GetMapping("/{articleId}")
    public ApiResponse<NewsReadStore.NewsArticleView> article(@PathVariable UUID articleId) {
        return ApiResponse.success(requestId(), store.article(articleId)
                .orElseThrow(() -> new NewsResourceNotFoundException(articleId)));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private <T> ApiResponse<PaginatedResponse<T>> paginated(List<T> rows, int limit, int offset) {
        boolean hasNext = rows.size() > limit;
        List<T> items = hasNext ? rows.subList(0, limit) : rows;
        return ApiResponse.success(requestId(), new PaginatedResponse<>(items, limit, offset, hasNext));
    }

    private String requestId() {
        return MDC.get(com.motogp.intelligence.common.CorrelationIdFilter.MDC_KEY);
    }
}
