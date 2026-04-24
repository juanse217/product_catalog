package com.sebastian.dev.productcatalog.controller.dto;

import java.util.List;

public record PagedResponse<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    boolean hasNext

) {}
