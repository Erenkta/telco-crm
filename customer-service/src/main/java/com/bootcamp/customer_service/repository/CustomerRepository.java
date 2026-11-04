package com.bootcamp.customer_service.repository;

import com.bootcamp.customer_service.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

    // Detail view: customer + addresses in one query. Safe because it returns a single customer, no pagination.
    // Documents are left lazy (one extra query when mapped); fetching two List collections in one query
    // would throw MultipleBagFetchException.
    @Query("select c from Customer c left join fetch c.addresses where c.id = :id")
    Optional<Customer> findWithAddressesById(@Param("id") UUID id);

    // Adding/removing an address or document does not touch the customers row, so @Version would not move.
    // OPTIMISTIC_FORCE_INCREMENT bumps the customer's version at commit anyway: child changes count as a change
    // of the customer (ETag changes, concurrent child changes conflict). Same pattern as identity's role assignment.
    // No join fetch here: the lock mode applies to EVERY entity the query returns, and Address has no @Version
    // ("Entity Address has no version and may not be locked"). Addresses load lazily when needed instead.
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select c from Customer c where c.id = :id")
    Optional<Customer> findByIdForUpdate(@Param("id") UUID id);
}
