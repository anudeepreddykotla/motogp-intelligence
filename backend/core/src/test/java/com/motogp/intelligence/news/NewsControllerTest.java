package com.motogp.intelligence.news;

import com.motogp.intelligence.common.CorrelationIdFilter;
import com.motogp.intelligence.common.GlobalExceptionHandler;
import com.motogp.intelligence.system.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NewsController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, GlobalExceptionHandler.class})
class NewsControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    NewsReadStore store;

    @Test
    void returnsFullPageWithNextMetadata() throws Exception {
        given(store.articles(null, null, null, null, 2, 0)).willReturn(List.of(article("one"), article("two"), article("three")));

        mockMvc.perform(get("/api/v1/news").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.limit").value(2))
                .andExpect(jsonPath("$.data.offset").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    void returnsPartialPageWithoutNextMetadata() throws Exception {
        given(store.articles(null, null, null, null, 2, 2)).willReturn(List.of(article("last")));

        mockMvc.perform(get("/api/v1/news").param("limit", "2").param("offset", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.offset").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void returnsEmptyPage() throws Exception {
        given(store.articles(null, null, null, null, 2, 4)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/news").param("limit", "2").param("offset", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void passesFiltersAndPreservesNullableFields() throws Exception {
        given(store.articles("race", "Fixture RSS", Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-31T23:59:59Z"), 20, 0)).willReturn(List.of(
                new NewsReadStore.NewsArticleView(UUID.randomUUID(), "Race update", "Fixture RSS", null,
                        null, null, "https://example.test/article")));

        mockMvc.perform(get("/api/v1/news")
                        .param("title", " race ").param("publisher", " Fixture RSS ")
                        .param("publishedFrom", "2026-01-01T00:00:00Z")
                        .param("publishedTo", "2026-01-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].publishedAt").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].permittedExcerpt").value(nullValue()));
    }

    @Test
    void rejectsInvertedPublicationDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/news")
                        .param("publishedFrom", "2026-02-01T00:00:00Z")
                        .param("publishedTo", "2026-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"));
    }

    @Test
    void returnsArticleDetails() throws Exception {
        UUID id = UUID.randomUUID();
        given(store.article(id)).willReturn(Optional.of(article("detail")));

        mockMvc.perform(get("/api/v1/news/{articleId}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("detail"));
    }

    @Test
    void rejectsMalformedArticleId() throws Exception {
        mockMvc.perform(get("/api/v1/news/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"));
    }

    @Test
    void returnsNotFoundForUnknownArticle() throws Exception {
        UUID id = UUID.randomUUID();
        given(store.article(id)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/news/{articleId}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"));
    }

    private NewsReadStore.NewsArticleView article(String title) {
        return new NewsReadStore.NewsArticleView(UUID.randomUUID(), title, "Fixture RSS",
                Instant.parse("2026-01-01T00:00:00Z"), "Excerpt", null, "https://example.test/article");
    }
}
