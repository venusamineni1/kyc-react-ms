package com.venus.kyc.viewer;

import java.util.List;

public record PaginatedResponse<T>(
        List<T> content,
        int currentPage,
        int pageSize,
        long totalElements,
        int totalPages) {
}
