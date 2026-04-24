package com.sebastian.dev.productcatalog.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import com.sebastian.dev.productcatalog.exception.ProductNotFoundException;
import com.sebastian.dev.productcatalog.model.document.Product;
import com.sebastian.dev.productcatalog.repository.ProductRepository;

@Service
public class ProductService {
    
    private final ProductRepository repo;

    public ProductService(ProductRepository repo){
        this.repo = repo;
    }

    public List<Product> findAllProducts(){
        return repo.findAll(); //Not returning defensive copy. Unnecessary for operations. We simply send info to the controller and no information in this list will be persisted or be used by other components. 
    }

    public Product findProductById(String id){
        return repo.findById(id).orElseThrow(() -> new ProductNotFoundException("The product with id " + id + " not found"));
    }

    public Product createProduct(Product p){
        return repo.save(p);
    }

    public Product updateProduct(Product p, String id){

        Product updateable = findProductById(id);

        if(p.getName() != null) updateable.setName(p.getName());
        if(p.getPrice() != null) updateable.setPrice(p.getPrice());
        if(p.getDescription() != null) updateable.setDescription(p.getDescription());
        if(p.getSpecifications() != null) updateable.setSpecifications(p.getSpecifications());
        if(p.getTags() != null) updateable.setTags(p.getTags());

        return repo.save(updateable);
    } 

    public void deleteProduct(String id){
        if (!repo.existsById(id)){
            throw new ProductNotFoundException("The product with id " + id + " not found for deletion");
        }
        repo.deleteById(id);
    }

    public List<Product> findProductByNameOrDescriptionContaining(String word){
        return repo.findProductsWithWord(word);
    }

    public Slice<Product> findProductsByTag(String tag, Pageable pageable){
        return repo.findByTagsContaining(tag, pageable);
    }

    public Slice<Product> findProductBySpecificationKeyAndValue(String key, String value, Pageable pageable){
        return repo.findBySpecificationKeyAndValue(key, value, pageable);
    }

    public Slice<Product> findProductInPriceRange(BigDecimal min, BigDecimal max, Pageable pageable){
        return repo.findByPriceBetween(min, max, pageable);
    }

}
