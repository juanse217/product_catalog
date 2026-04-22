package com.sebastian.dev.productcatalog.model.document;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {
    @Id
    private String id;
    @TextIndexed
    private String name; 
    private BigDecimal price;
    @TextIndexed
    private String description; 
    private Map<String, String> specifications;
    private Set<String> tags;

}
