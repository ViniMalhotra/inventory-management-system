package com.inventory.repository;

import com.inventory.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * ShipmentRepository - JPA repository for Shipment entity.
 */
@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    /**
     * Find shipments by order ID.
     */
    List<Shipment> findByOrderId(Long orderId);
}
