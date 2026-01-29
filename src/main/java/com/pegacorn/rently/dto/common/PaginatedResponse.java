package com.pegacorn.rently.dto.common;

import java.util.List;

public record PaginatedResponse<T>(
        boolean success,
        List<T> data,
        Pagination pagination
) {
    public record Pagination(
            int page,
            int limit,
            long total,
            int totalPages
    ) {}

    public static <T> PaginatedResponse<T> of(List<T> data, int page, int limit, long total) {
        int totalPages = (int) Math.ceil((double) total / limit);
        return new PaginatedResponse<>(true, data, new Pagination(page, limit, total, totalPages));
    }
}
