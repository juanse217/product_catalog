package com.sebastian.dev.productcatalog.controller.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

//Using validation groups to allow null values on PUT operations.
public record ProductDTO(
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    String id,
    @NotBlank(groups = OnPost.class ,message = "The name is required")
    @Size(min = 5, max = 100, message = "The length of the name must be between 5 and 100 characters")
    String name,
    @Min(1)
    BigDecimal price,
    @NotBlank(groups = OnPost.class, message = "The description is required")
    @Size(min = 5, max = 300, message = "The length of the description must be between 5 and 300 characters")
    String description, 
    @NotNull(groups = OnPost.class, message = "You must provide at least one specification for the product")
    @Size(min = 1, max = 15, message = "You need to provide between 1 and 15 specifications for this product")
    Map<String, String> specifications,
    @NotNull(groups = OnPost.class, message = "You must provide at least one tag for the product")
    @Size(min = 1, max = 5, message = "You need to provide between 1 and 5 tags for this product")
    Set<String> tags
) {
    public interface OnPost{}
}
