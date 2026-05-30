package com.maintaintrack.api.repositories;

import com.maintaintrack.api.models.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface PartRepository extends JpaRepository<Part, Long> {
    @Query("SELECT p FROM Part p WHERE p.qtyOnHand <= p.minQty")
    List<Part> findLowStock();
    Optional<Part> findBySyncId(String syncId);
}