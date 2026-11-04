package com.bootcamp.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// Bound from app.pageable.* by @EnableConfigurationProperties in CommonWebAutoConfiguration.
// The checks below run once at startup: a misconfigured service fails to start instead of returning 500s later.
@ConfigurationProperties("app.pageable")
public record PageableProperties(int defaultPageSize, int maxPageSize, String defaultSortBy, List<String> sortWhitelist){

    public PageableProperties {
        if (sortWhitelist == null || sortWhitelist.isEmpty()) {
            throw new IllegalStateException("app.pageable.sort-whitelist must be set");
        }
        if (defaultSortBy == null || !sortWhitelist.contains(defaultSortBy)) {
            throw new IllegalStateException("app.pageable.default-sort-by (" + defaultSortBy + ") must be one of app.pageable.sort-whitelist " + sortWhitelist);
        }
        if (defaultPageSize < 1 || maxPageSize < defaultPageSize) {
            throw new IllegalStateException("app.pageable sizes must satisfy 1 <= default-page-size <= max-page-size, got "
                    + defaultPageSize + " and " + maxPageSize);
        }
        sortWhitelist = List.copyOf(sortWhitelist);
    }
}
