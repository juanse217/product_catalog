## Spring Data MongoDB: Querying Dynamic Maps

When working with a `Map<String, String>` (e.g., `specifications`), use these two approaches depending on whether the map key is known.

### 1. Specific Key Search (Fixed Key)
Use this when you know the exact key name (e.g., "RAM") and want to filter by its value.
*   **Best for:** Fixed filters or dropdown-based searches.
*   **Performance:** High (can be indexed).

```java
@Query("{ 'specifications.RAM': { $regex: ?0, $options: 'i' } }")
List<Product> findByRamValue(String valuePattern);
```
### Dynamic Key & Value Search (Unknown Key)
Use this to search across any key name and any value within the map.
* Best for: Global search bars or highly dynamic schemas where keys vary per document.
* Mechanism: Converts the Map to an array of { k, v } pairs using $objectToArray to allow regex on keys.

``` 
@Aggregation(pipeline = {
    "{ $addFields: { specsArray: { $objectToArray: '$specifications' } } }",
    "{ $match: { 'specsArray.k': { $regex: ?0, $options: 'i' }, " + 
              "  'specsArray.v': { $regex: ?1, $options: 'i' } } }"
})
List<Product> findByAnySpec(String keyRegex, String valueRegex);
```
