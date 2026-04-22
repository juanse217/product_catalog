package com.sebastian.dev.productcatalog.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.sebastian.dev.productcatalog.model.document.Product;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

        @Query("""
                { '$text': {'$search': ?0 }}
                """)
        List<Product> findProductsWithWord(String word);

        Slice<Product> findByTagsContaining(String tag, Pageable pageable);

        Slice<Product> findByPriceBetween(int min, int max, Pageable pageable);

        @Aggregation(pipeline = {
                 // 1. Convert the Map to an array for searching keys/values
                "{ $addFields: { specsArray: { $objectToArray: '$specifications' } } }",

                // 2. Search for the word inside ANY key or ANY value
                "{ $match: { $and: [ " +
                                "  { 'specsArray.k': { $regex: ?0, $options: 'i' } }, " +
                                "  { 'specsArray.v': { $regex: ?1, $options: 'i' } } " +
                                "   ] } }"
        })
        Slice<Product> findBySpecificationKeyAndValue(String key, String val, Pageable pageable);
}
