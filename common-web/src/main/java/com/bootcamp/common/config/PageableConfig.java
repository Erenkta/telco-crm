package com.bootcamp.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

// To activate the custom Pageable parameter resolver.
@RequiredArgsConstructor
public class PageableConfig implements WebMvcConfigurer {

    private final PageableProperties pageProps;


    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(0, new PageSizeLimitedPageableResolver(
                pageProps.defaultPageSize(),
                pageProps.maxPageSize(),
                Sort.by(pageProps.defaultSortBy()),
                pageProps.sortWhitelist()
                )
        );
    }

}
