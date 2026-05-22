package com.maintaintrack.api.repositories;

import com.maintaintrack.api.models.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {}