package com.sebastian.dev.productcatalog.mapper;


import com.sebastian.dev.productcatalog.controller.dto.ProductDTO;
import com.sebastian.dev.productcatalog.model.document.Product;

public class ProductDTOMapper {

    public static Product toProduct(ProductDTO dto){
        Product p = new Product();
        p.setName(dto.name());
        p.setDescription(dto.description());
        p.setPrice(dto.price());
        p.setSpecifications(dto.specifications());
        p.setTags(dto.tags());

        return p;
    }

    public static ProductDTO toProductDTO(Product p){
        ProductDTO dto = new ProductDTO(p.getId(), p.getName(), p.getPrice(), p.getDescription(), p.getSpecifications(), p.getTags());

        return dto;
    }
}
