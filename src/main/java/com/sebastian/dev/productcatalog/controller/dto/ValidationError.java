package com.sebastian.dev.productcatalog.controller.dto;

public record ValidationError(
    String field, 
    String message
) {

}
