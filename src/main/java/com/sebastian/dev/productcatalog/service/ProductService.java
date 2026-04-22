package com.sebastian.dev.productcatalog.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
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
        if(p == null){
            throw new IllegalArgumentException("The product entity cannot be null");
        }

        return repo.save(p);
    }

    public Product updateProduct(Product p, String id){
        if(p == null){
            throw new IllegalArgumentException("The product entity cannot be null");
        }
        if(id == null || id.isBlank()){
            throw new IllegalArgumentException("The id is required for updating the product");
        }

        Product updateable = findProductById(id);

        if(p.getName() != null) updateable.setName(p.getName());
        if(p.getPrice() != null) updateable.setPrice(p.getPrice());
        if(p.getDescription() != null) updateable.setDescription(p.getDescription());
        if(p.getSpecifications() != null) updateable.setSpecifications(p.getSpecifications());
        if(p.getTags() != null) updateable.setTags(p.getTags());

        return repo.save(updateable);
    } 

    public void deleteProduct(String id){
        Product found = findProductById(id);
        repo.delete(found);
    }

    public List<Product> findProductByNameOrDescriptionContaining(String word){
        return repo.findProductsWithWord(word);
    }

    public List<Product> findProductsByTag(String tag, Pageable pageable){
        return repo.findByTagsContaining(tag, pageable).getContent();
    }

    public List<Product> findProductBySpecificationKeyAndValue(String key, String value, Pageable pageable){
        return repo.findBySpecificationKeyAndValue(key, value, pageable).getContent();
    }

    public List<Product> findProductInPriceRange(int min, int max, Pageable pageable){
        return repo.findByPriceBetween(min, max, pageable).getContent();
    }

}
