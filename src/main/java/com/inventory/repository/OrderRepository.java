package com.inventory.repository;

import com.inventory.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * OrderRepository - JPA repository for Order entity.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
