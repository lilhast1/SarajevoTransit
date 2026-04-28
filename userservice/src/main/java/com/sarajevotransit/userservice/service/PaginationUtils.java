package com.sarajevotransit.userservice.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

final class PaginationUtils {

    private PaginationUtils() {
    }

    static Pageable buildPageable(
            int page,
            int size,
            String sort,
            String defaultSortField,
            Sort.Direction defaultDirection,
            Set<String> allowedSortFields) {

        String sortField = defaultSortField;
        Sort.Direction direction = defaultDirection;

        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            if (parts.length > 0 && !parts[0].isBlank()) {
                sortField = parts[0].trim();
            }
            if (parts.length == 2 && !parts[1].isBlank()) {
                try {
                    direction = Sort.Direction.fromString(parts[1].trim());
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'.");
                }
            }
        }

        if (!allowedSortFields.contains(sortField)) {
            throw new IllegalArgumentException("Invalid sort field: " + sortField);
        }

        return PageRequest.of(page, size, Sort.by(direction, sortField));
    }
}
