package com.evaitcs.securebank.payment.client;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

/**
 * THE SERVICE-TO-SERVICE CLIENT — payment-service's ONLY door to account data.
 * ============================================================================
 * Compare with stage-2's TransferService: there, "call the account layer" was
 * a method call. Here it is an HTTP call through a discovery-resolved RestClient,
 * wrapped in Resilience4j. Everything this class exists to handle — latency,
 * partial failure, cascading outages — simply could not happen in-process.
 * That new failure surface IS the cost side of the microservices ledger.
 *
 * DISCOVERY, EXPLICITLY (see PaymentServiceApplication for the war story):
 * each call first asks the LoadBalancerClient to choose a healthy
 * "account-service" instance from the Eureka registry (round-robin across
 * replicas), then targets that concrete host:port. This is the same machinery
 * @LoadBalanced automates — done by hand so it neither leaks a magic builder
 * bean into the context (which hijacks the Eureka client's own transport!)
 * nor hides the lookup that students need to SEE to believe.
 *
 * RESILIENCE4J ANNOTATIONS (evaluated by AOP; order: Retry OUTSIDE CircuitBreaker):
 *
 *   @Retry(name="account-service")
 *     transient blips (one dropped packet, a restarting instance) get
 *     max-attempts tries with a wait between them. Config in
 *     config-repo/payment-service.yml; ignore-exceptions excludes BUSINESS
 *     refusals — retrying "insufficient funds" three times would be silly.
 *
 *   @CircuitBreaker(name="account-service", fallbackMethod=...)
 *     counts failures in a sliding window; past the threshold it OPENS:
 *     calls stop going out (fail fast in milliseconds) and the fallback runs
 *     instead, giving the sick service room to recover. After the wait
 *     duration it half-opens and probes. This is how you prevent ONE dead
 *     dependency from exhausting your threads and taking YOU down too
 *     (cascade failure — the classic outage post-mortem).
 *
 * FALLBACK CONTRACT: same signature + trailing Throwable; it TRANSLATES the
 * failure into our typed exceptions so the saga can branch on
 * business-vs-technical (see InsufficientFundsException comment).
 * ============================================================================
 */
@Component
public class AccountClient {

    /** The account-service view/movement shapes (mirrors its Dtos — the two
     *  services share a CONTRACT, not code; OpenAPI/codegen would generate
     *  these from the producer's spec in a bigger estate). */
    public record AccountView(Long id, Long customerId, String type, BigDecimal balance) {
    }

    public record MoneyMovement(Long accountId, BigDecimal newBalance) {
    }

    private record MoneyMovementRequest(BigDecimal amount, String reference, String description) {
    }

    private static final String SERVICE_ID = "account-service"; // the Eureka registration name

    private final LoadBalancerClient loadBalancer;
    private final RestClient restClient; // plain (non-bean) client; URLs are absolute per call

    public AccountClient(LoadBalancerClient loadBalancer) {
        this.loadBalancer = loadBalancer;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Ask the registry for a healthy instance and build its absolute base URL.
     * choose() round-robins: scale account-service to N replicas and each call
     * lands on the next one — client-side load balancing in three lines.
     */
    private String baseUrl() {
        ServiceInstance instance = loadBalancer.choose(SERVICE_ID);
        if (instance == null) {
            throw new AccountServiceUnavailableException(
                    "No instances of '" + SERVICE_ID + "' in the registry (is it running? registered?)");
        }
        return "http://" + instance.getHost() + ":" + instance.getPort();
    }

    /** Ownership/balance probe before moving money. Read-only: no @Retry
     *  storm risk, but the circuit still guards a down service. */
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(
            name = "account-service", fallbackMethod = "viewFallback")
    public AccountView view(long accountId) {
        try {
            return restClient.get()
                    .uri(baseUrl() + "/internal/accounts/{id}", accountId)
                    .retrieve()
                    .body(AccountView.class);
        } catch (HttpClientErrorException.BadRequest e) {
            throw new IllegalStateException("Unexpected 400 on account view", e);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new AccountUnknownException(accountId);
            }
            throw new AccountServiceUnavailableException(
                    "account-service responded " + e.getStatusCode(), e);
        }
    }

    @io.github.resilience4j.retry.annotation.Retry(name = "account-service")
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(
            name = "account-service", fallbackMethod = "debitFallback")
    public MoneyMovement debit(long accountId, BigDecimal amount, String reference, String description) {
        return move("debit", accountId, amount, reference, description);
    }

    @io.github.resilience4j.retry.annotation.Retry(name = "account-service")
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(
            name = "account-service", fallbackMethod = "creditFallback")
    public MoneyMovement credit(long accountId, BigDecimal amount, String reference, String description) {
        return move("credit", accountId, amount, reference, description);
    }

    private MoneyMovement move(String verb, long accountId, BigDecimal amount,
                               String reference, String description) {
        try {
            return restClient.post()
                    .uri(baseUrl() + "/internal/accounts/{id}/{verb}", accountId, verb)
                    .body(new MoneyMovementRequest(amount, reference, description))
                    .retrieve()
                    .body(MoneyMovement.class);
        } catch (HttpClientErrorException.BadRequest e) {
            // account-service answered 400 problem+json — a BUSINESS refusal.
            // Surface the server's detail so the client sees the real reason.
            throw new InsufficientFundsException(extractDetail(e));
        } catch (RestClientResponseException e) {
            throw new AccountServiceUnavailableException(
                    "account-service /" + verb + " responded " + e.getStatusCode(), e);
        }
    }

    private static String extractDetail(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        int i = body.indexOf("\"detail\":\"");
        if (i < 0) {
            return "account-service refused the movement";
        }
        int start = i + "\"detail\":\"".length();
        int end = body.indexOf('"', start);
        return end > start ? body.substring(start, end) : "account-service refused the movement";
    }

    // --- fallbacks: translate resilience failures into typed exceptions -----
    // (the original cause rides along for logging)

    @SuppressWarnings("unused") // invoked reflectively by Resilience4j
    private AccountView viewFallback(long accountId, Throwable t) {
        throw asTyped("view account " + accountId, t);
    }

    @SuppressWarnings("unused")
    private MoneyMovement debitFallback(long accountId, BigDecimal amount, String reference,
                                        String description, Throwable t) {
        if (t instanceof InsufficientFundsException) {
            throw (InsufficientFundsException) t;   // business refusal passes through untouched
        }
        throw asTyped("debit account " + accountId, t);
    }

    @SuppressWarnings("unused")
    private MoneyMovement creditFallback(long accountId, BigDecimal amount, String reference,
                                         String description, Throwable t) {
        if (t instanceof InsufficientFundsException) {
            throw (InsufficientFundsException) t;
        }
        throw asTyped("credit account " + accountId, t);
    }

    private static RuntimeException asTyped(String what, Throwable t) {
        // Already-typed failures (including "no instances" from baseUrl) pass through.
        if (t instanceof AccountServiceUnavailableException || t instanceof AccountUnknownException) {
            return (RuntimeException) t;
        }
        Throwable cause = t.getCause() != null ? t.getCause() : t; // unwrap resilience wrappers
        if (cause instanceof AccountServiceUnavailableException || cause instanceof AccountUnknownException) {
            return (RuntimeException) cause;
        }
        return new AccountServiceUnavailableException("account-service unavailable during " + what, cause);
    }

    /** Unknown account id (404 from the internal view). */
    public static class AccountUnknownException extends RuntimeException {
        public AccountUnknownException(long accountId) {
            super("Account " + accountId + " does not exist");
        }
    }
}
