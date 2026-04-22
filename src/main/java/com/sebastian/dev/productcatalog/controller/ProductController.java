package com.sebastian.dev.productcatalog.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sebastian.dev.productcatalog.controller.dto.ProductDTO;
import com.sebastian.dev.productcatalog.controller.dto.ProductDTO.OnPost;
import com.sebastian.dev.productcatalog.controller.mapper.ProductDTOMapper;
import com.sebastian.dev.productcatalog.model.document.Product;
import com.sebastian.dev.productcatalog.service.ProductService;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;




@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;
    private final ProductDTOMapper mapper;

    public ProductController(ProductService service, ProductDTOMapper mapper){
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> findAllProducts() {
        return ResponseEntity.ok(service.findAllProducts()
                                        .stream()
                                        .map(mapper::toProductDTO)
                                        .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> findProductById(@PathVariable String id) {
        return ResponseEntity.ok(mapper.toProductDTO(service.findProductById(id)));
    }

    @GetMapping("/word")
    public ResponseEntity<List<ProductDTO>> findAllContainingWord(@RequestParam String word) {
        return ResponseEntity.ok(service.findProductByNameOrDescriptionContaining(word)
                                        .stream()
                                        .map(mapper::toProductDTO)
                                        .toList());
    }

    @GetMapping("/tag")
    public ResponseEntity<List<ProductDTO>> findAllContainingTag(@RequestParam String tag, Pageable pageable) {
        return ResponseEntity.ok(service.findProductsByTag(tag, pageable)
                                        .stream()
                                        .map(mapper::toProductDTO)
                                        .toList());
    }
    
    @GetMapping("/specifications")
    public ResponseEntity<List<ProductDTO>> findByKeyAndValue(@RequestParam String key, @RequestParam String value, Pageable pageable) {
        return ResponseEntity.ok(
                service.findProductBySpecificationKeyAndValue(key, value, pageable)
                        .stream()
                        .map(mapper::toProductDTO)
                        .toList()
            );
    }
    
    @GetMapping("range")
    public ResponseEntity<List<ProductDTO>> findByRange(@RequestParam Integer min, @RequestParam Integer max, Pageable pageable) {
        return ResponseEntity.ok(
            service.findProductInPriceRange(min, max, pageable)
                    .stream()
                    .map(mapper::toProductDTO)
                    .toList()
        );
    }
    
    
    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@Validated(OnPost.class) @RequestBody ProductDTO dto) {
        Product saved = service.createProduct(mapper.toProduct(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toProductDTO(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable String id, @Valid @RequestBody ProductDTO dto) {
        Product updated = service.updateProduct(mapper.toProduct(dto), id);

        return ResponseEntity.ok(mapper.toProductDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id){
        service.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    
}
