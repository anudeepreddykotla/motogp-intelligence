package com.motogp.intelligence.common;

import java.util.List;

public record PaginatedResponse<T>(List<T> items, int limit, int offset, boolean hasNext) {
}
