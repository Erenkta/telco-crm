package com.bootcamp.identity_service.config;

import com.bootcamp.identity_service.exception.InvalidPageSizeException;
import com.bootcamp.identity_service.exception.InvalidPageSortException;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

// Once a HTTP request hits, Spring ask argument resolvers to fill the controllers parameters.
// Each resolver is handle a specific type of an argument.
public class PageSizeLimitedPageableResolver extends PageableHandlerMethodArgumentResolver {
    private final int maxPageSize;
    private final List<String> sortWhiteList = List.of("createdAt","updatedAt");

    public PageSizeLimitedPageableResolver(int defaultPageSize,int maxPageSize,Sort sortByDefault){
        this.maxPageSize = maxPageSize;
        this.setMaxPageSize(Integer.MAX_VALUE); // To disable the behavior of Parent class.
        setFallbackPageable(PageRequest.of(0,defaultPageSize, sortByDefault)); // if page and size are never sent as params.
    }

    @Override
    public Pageable resolveArgument(MethodParameter parameter,
                                    ModelAndViewContainer mavContainer,
                                    NativeWebRequest webRequest,
                                    WebDataBinderFactory binderFactory){
        Pageable pageable = super.resolveArgument(parameter, mavContainer, webRequest, binderFactory);

        pageable.getSort().stream().forEach(item -> { // To check if sort parameter is valid
            String sortProperty = item.getProperty();
            if(!sortWhiteList.contains(sortProperty)){
                String message = String.format("%s is not a valid sort parameter.",sortProperty);
                throw new InvalidPageSortException(message);
            }
        });

        if (pageable.isPaged() && pageable.getPageSize() > maxPageSize) {
            String message = String.format("Max page size (%s) is exceeded with parameter %s",maxPageSize,pageable.getPageSize());
            throw new InvalidPageSizeException(message);
        }

        if(pageable.getSort().stream().noneMatch(item -> item.getProperty().equals("id"))){ // To check tie-breaker
            return PageRequest.of(pageable.getPageNumber(),pageable.getPageSize(),pageable.getSort().and(Sort.by("id")));
        }
        return pageable;
    }
}
