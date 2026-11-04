package com.bootcamp.common.dto;

import org.springframework.data.domain.Page;

import java.util.Collection;

public record PageResponse<T>(
        int pageSize,
        int pageNo,
        Collection<T> content,
        long totalElements,
        int totalPages
) {
    public PageResponse(Page<T> pageResp){
        this(pageResp.getSize(), pageResp.getNumber(), pageResp.getContent(), pageResp.getTotalElements(), pageResp.getTotalPages());
    }
}
