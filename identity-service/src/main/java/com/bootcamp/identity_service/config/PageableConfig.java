package com.bootcamp.identity_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

// To activate the custom Pageable parameter resolver.
@Configuration
public class PageableConfig implements WebMvcConfigurer {
    @Value("${app.pageable.default-page-size:20}")
    private int defaultPageSize;

    @Value("${app.pageable.max-page-size:1000}")
    private int maxPageSize;

    @Value("${app.pageable.default-sort-by}")
    private String defaultSortBy;


    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(0, new PageSizeLimitedPageableResolver(
                defaultPageSize,
                maxPageSize,
                Sort.by(defaultSortBy))
        );
    }

}
