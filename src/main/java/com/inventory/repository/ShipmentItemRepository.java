package com.inventory.repository;

import com.inventory.entity.ShipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * ShipmentItemRepository - JPA repository for ShipmentItem entity.
 */
@Repository
public interface ShipmentItemRepository extends JpaRepository<ShipmentItem, Long> {
    /**
     * Find shipment items by shipment ID.
     */
    List<ShipmentItem> findByShipmentId(Long shipmentId);
}
