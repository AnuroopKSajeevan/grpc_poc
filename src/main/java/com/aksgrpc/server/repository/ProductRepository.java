package com.aksgrpc.server.repository;

import com.aksgrpc.server.entity.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    Optional<Product> findById(String id);

    List<Product> findByActiveTrue();

    List<Product> findByCategory(String category);

    List<Product> findByActiveTrueAndCategory(String category);

    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    List<Product> findByNameContainingIgnoreCase(String name);

    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'price': { $lte: ?1 } }")
    List<Product> searchByNameAndMaxPrice(String name, double maxPrice);

    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'quantity': { $gte: ?1 } }")
    List<Product> searchByNameAndMinQuantity(String name, int minQuantity);
}
