package com.bootcamp.customer_service.service;

import com.bootcamp.common.exception.PreconditionFailedException;
import com.bootcamp.customer_service.TestcontainersConfiguration;
import com.bootcamp.customer_service.dto.request.AddAddressRequest;
import com.bootcamp.customer_service.dto.request.CreateIndividualCustomerRequest;
import com.bootcamp.customer_service.dto.response.CustomerDetailResponse;
import com.bootcamp.customer_service.entity.enums.KycStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Proves the two protection layers of conditional customer updates against a real Postgres:
//   layer 1: If-Match version check in the service  -> PreconditionFailedException (412)
//   layer 2: @Version in the UPDATE's WHERE clause  -> ObjectOptimisticLockingFailureException (409)
// No @Transactional on the test: every service call commits for real, like two HTTP requests would.
@SpringBootTest
@Import({TestcontainersConfiguration.class, CustomerConcurrencyIT.BarrierClockConfig.class})
@ActiveProfiles("test")
class CustomerConcurrencyIT {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private BarrierClock barrierClock;

    @AfterEach
    void disarmClock() {
        barrierClock.disarm();
    }

    // The race the reviewer worried about: both requests read version 0 and pass the If-Match check.
    // The barrier clock holds both threads right after that check (approveKyc/rejectKyc call clock.instant()
    // there), so both really are past line "version == expectedVersion" before either one writes.
    // Then both flush: UPDATE customers ... WHERE id = ? AND version = 0
    // The second UPDATE waits for the first transaction's row lock, re-checks the WHERE clause after commit,
    // matches 0 rows, and Hibernate turns that into an optimistic locking failure.
    @Test
    void concurrentApproveAndReject_exactlyOneWins_whenBothPassedTheIfMatchCheck() throws Exception {
        UUID id = createCustomer().id();
        barrierClock.armFor(2);

        List<Outcome> outcomes = runConcurrently(
                () -> customerService.approveKyc(id, 0),
                () -> customerService.rejectKyc(id, 0, "forged document"));

        List<Outcome> winners = outcomes.stream().filter(o -> o.result() != null).toList();
        List<Outcome> losers = outcomes.stream().filter(o -> o.error() != null).toList();
        assertThat(winners).hasSize(1);
        assertThat(losers).hasSize(1);
        assertThat(losers.getFirst().error()).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        barrierClock.disarm();
        CustomerDetailResponse stored = customerService.getById(id);
        CustomerDetailResponse winner = winners.getFirst().result();
        assertThat(stored.version()).isEqualTo(1L);
        assertThat(stored.kycStatus()).isEqualTo(winner.kycStatus());
    }

    // Layer 1 alone: operator B acts minutes later with the ETag from before operator A's approval.
    // @Version could not catch this (B's transaction reads the fresh version); the If-Match check does.
    @Test
    void reject_throwsPreconditionFailed_whenIfMatchVersionIsStale() {
        UUID id = createCustomer().id();

        customerService.approveKyc(id, 0);

        assertThatThrownBy(() -> customerService.rejectKyc(id, 0, "forged document"))
                .isInstanceOf(PreconditionFailedException.class);
        CustomerDetailResponse stored = customerService.getById(id);
        assertThat(stored.kycStatus()).isEqualTo(KycStatus.VERIFIED);
        assertThat(stored.version()).isEqualTo(1L);
    }

    // OPTIMISTIC_FORCE_INCREMENT: an address change does not touch the customers row, but still bumps its version,
    // so an operator holding the old ETag cannot approve KYC without having seen the new address.
    @Test
    void addAddress_incrementsCustomerVersion() {
        UUID id = createCustomer().id();

        customerService.addAddress(id, new AddAddressRequest("Home", "Bagdat Cd. 1", "Kadikoy", "Istanbul", "34710", null));

        assertThat(customerService.getById(id).version()).isEqualTo(1L);
        assertThatThrownBy(() -> customerService.approveKyc(id, 0))
                .isInstanceOf(PreconditionFailedException.class);
    }

    private CustomerDetailResponse createCustomer() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String nationalId = String.valueOf(10_000_000_000L + ThreadLocalRandom.current().nextLong(89_999_999_999L));
        return customerService.create(new CreateIndividualCustomerRequest(
                "it-" + suffix + "@mail.com", "+905551112233", "Test", "Customer", nationalId));
    }

    @SafeVarargs
    private static List<Outcome> runConcurrently(Callable<CustomerDetailResponse>... calls) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(calls.length);
        try {
            List<Future<CustomerDetailResponse>> futures = new ArrayList<>();
            for (Callable<CustomerDetailResponse> call : calls) {
                futures.add(pool.submit(call));
            }
            List<Outcome> outcomes = new ArrayList<>();
            for (Future<CustomerDetailResponse> future : futures) {
                try {
                    outcomes.add(new Outcome(future.get(30, TimeUnit.SECONDS), null));
                } catch (ExecutionException e) {
                    outcomes.add(new Outcome(null, e.getCause()));
                } catch (java.util.concurrent.TimeoutException e) {
                    throw new IllegalStateException("Concurrent call did not finish in time", e);
                }
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    private record Outcome(CustomerDetailResponse result, Throwable error) {
    }

    // A Clock that, when armed, makes the first N callers of instant() wait for each other.
    // The service asks the clock for "now" right after the If-Match check, which makes it a natural sync point.
    static class BarrierClock extends Clock {

        private volatile CyclicBarrier barrier;

        void armFor(int parties) {
            barrier = new CyclicBarrier(parties);
        }

        void disarm() {
            barrier = null;
        }

        @Override
        public Instant instant() {
            CyclicBarrier current = barrier;
            if (current != null) {
                try {
                    current.await(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException("Concurrent requests did not meet at the barrier", e);
                }
            }
            return Instant.now();
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class BarrierClockConfig {
        @Bean
        @Primary
        BarrierClock barrierClock() {
            return new BarrierClock();
        }
    }
}
