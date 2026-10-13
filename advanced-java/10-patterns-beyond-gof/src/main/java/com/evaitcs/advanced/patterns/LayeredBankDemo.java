package com.evaitcs.advanced.patterns;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Demo A1 — LAYERED ARCHITECTURE, mirrored from the real stage-2-monolith.
 * (README section "Layered: controller -> service -> repository".)
 *
 * stage-2-monolith's actual package shape is:
 *   com.evaitcs.securebank.web        (TransferController, GlobalExceptionHandler)
 *   com.evaitcs.securebank.web.dto    (TransferRequest, TransferResponse records)
 *   com.evaitcs.securebank.service    (TransferService)
 *   com.evaitcs.securebank.repository (AccountRepository)
 *   com.evaitcs.securebank.domain     (Account entity, InsufficientFundsException)
 *
 * This demo is that shape in plain Java, no Spring: each layer talks ONLY to the one
 * below it, and the MUTABLE entity dies at the service boundary — only frozen DTO
 * records cross it. The test asserts the returned object's CLASS to prove the leak
 * never happens.
 *
 * Layer jobs, one line each:
 *   controller — translate the outside world (request) into a service call and back;
 *   service    — business rules (overdraft!) and entity<->DTO mapping;
 *   repository — storage shaped like a collection (find/save), here a HashMap.
 */
public class LayeredBankDemo {

    // ---- DOMAIN layer: the entity. Package-private ON PURPOSE — code outside this
    // ---- package literally cannot reference the type, so it cannot leak. ----

    static final class AccountEntity {

        private final String id;
        private final String owner;
        private BigDecimal balance;          // mutable: only the service layer may move it

        AccountEntity(String id, String owner, BigDecimal balance) {
            this.id = id;
            this.owner = owner;
            this.balance = balance;
        }

        String id() {
            return id;
        }

        String owner() {
            return owner;
        }

        BigDecimal balance() {
            return balance;
        }

        void debit(BigDecimal amount) {
            balance = balance.subtract(amount);
        }

        void credit(BigDecimal amount) {
            balance = balance.add(amount);
        }
    }

    /** Same name stage-2-monolith uses: the exception type itself carries the meaning. */
    static final class InsufficientFundsException extends RuntimeException {

        InsufficientFundsException(String accountId, BigDecimal available, BigDecimal attempted) {
            super("account " + accountId + " has " + available.toPlainString()
                    + ", tried to send " + attempted.toPlainString());
        }
    }

    // ---- REPOSITORY layer: storage shaped like a collection. In stage-2 this is a
    // ---- Spring Data interface over PostgreSQL; here it is a HashMap. Same job. ----

    static final class AccountRepository {

        private final Map<String, AccountEntity> table = new HashMap<>();

        AccountRepository() {                 // seed data — stage-2's DataSeeder does this
            table.put("ACC-1", new AccountEntity("ACC-1", "Dana", new BigDecimal("500.00")));
            table.put("ACC-2", new AccountEntity("ACC-2", "Ana", new BigDecimal("100.00")));
        }

        void save(AccountEntity entity) {
            table.put(entity.id(), entity);
        }

        AccountEntity requireById(String id) {
            AccountEntity entity = table.get(id);
            if (entity == null) {
                throw new NoSuchElementException("no account " + id);
            }
            return entity;
        }
    }

    // ---- DTOs: frozen records that CROSS boundaries. Public, unlike the entity. ----

    /** What the outside world may see: no mutability, no storage details, no behavior. */
    public record AccountDto(String id, String owner, BigDecimal balance) { }

    /** The inbound request — stage-2 deserializes this from JSON; here we just construct it. */
    public record TransferRequest(String fromId, String toId, BigDecimal amount) { }

    /** The outbound receipt — every field exact and frozen. */
    public record TransferDto(String reference, String fromId, String toId, BigDecimal amount,
                              BigDecimal fromBalanceAfter, BigDecimal toBalanceAfter) { }

    // ---- SERVICE layer: business rules live HERE, and entity<->DTO mapping happens HERE ----

    static final class TransferService {

        private final AccountRepository repository;
        private int counter;

        TransferService(AccountRepository repository) {
            this.repository = repository;
        }

        TransferDto transfer(String fromId, String toId, BigDecimal amount) {
            AccountEntity from = repository.requireById(fromId);   // entities exist ONLY below here
            AccountEntity to = repository.requireById(toId);
            if (from.balance().compareTo(amount) < 0) {            // the business rule, in ONE place
                throw new InsufficientFundsException(fromId, from.balance(), amount);
            }
            from.debit(amount);
            to.credit(amount);
            repository.save(from);
            repository.save(to);
            counter++;
            // entity -> DTO mapping at the boundary: the entity stops HERE.
            return new TransferDto("TXN-" + counter, fromId, toId, amount, from.balance(), to.balance());
        }

        AccountDto accountView(String id) {
            AccountEntity entity = repository.requireById(id);
            return new AccountDto(entity.id(), entity.owner(), entity.balance());   // mapping again
        }
    }

    // ---- WEB layer: knows DTOs and the service. It has NEVER heard of AccountEntity. ----

    static final class TransferController {

        private final TransferService service;

        TransferController(TransferService service) {
            this.service = service;
        }

        TransferDto handle(TransferRequest request) {
            return service.transfer(request.fromId(), request.toId(), request.amount());
        }

        AccountDto handleView(String id) {
            return service.accountView(id);
        }
    }

    public static void main(String[] args) {
        banner("1. The real thing this mirrors: stage-2-monolith's packages");

        System.out.println("web/           TransferController, GlobalExceptionHandler   <- HTTP in/out");
        System.out.println("web/dto/       TransferRequest, TransferResponse (records)  <- what crosses");
        System.out.println("service/       TransferService                              <- business rules");
        System.out.println("repository/    AccountRepository                            <- storage access");
        System.out.println("domain/        Account entity, InsufficientFundsException   <- internal truth");
        System.out.println("verdict: arrows point DOWN only — web -> service -> repository -> domain.");

        banner("2. One transfer request, walked through every layer");

        AccountRepository repository = new AccountRepository();
        TransferService service = new TransferService(repository);
        TransferController controller = new TransferController(service);

        TransferRequest request = new TransferRequest("ACC-1", "ACC-2", new BigDecimal("250.00"));
        System.out.println("step 1: web        receives " + request.getClass().getSimpleName() + " " + request);
        TransferDto receipt = controller.handle(request);
        System.out.println("step 2: service    loaded BOTH entities, checked the overdraft rule, moved money");
        System.out.println("step 3: service    mapped entities -> " + receipt.getClass().getSimpleName());
        System.out.println("step 4: web        returns " + receipt);
        System.out.println("verdict: five numbered hops, and every layer only ever called the one below.");

        banner("3. The boundary proof: only DTO classes cross");

        AccountDto view = controller.handleView("ACC-1");
        System.out.println("step 1: controller.handleView(\"ACC-1\") returned class = " + view.getClass().getSimpleName());
        System.out.println("step 2: the entity class (AccountEntity) is package-private and NO method");
        System.out.println("        above the service ever returns it — the compiler enforces the boundary.");
        System.out.println("step 3: view = " + view);
        System.out.println("verdict: what crosses is a frozen record; what stays inside is mutable and guarded.");

        banner("4. The overdraft rule belongs to the service — the web layer just reports");

        try {
            controller.handle(new TransferRequest("ACC-1", "ACC-2", new BigDecimal("99999.00")));
        } catch (InsufficientFundsException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("why: the rule lives in ONE place (the service), so no controller, batch job");
            System.out.println("     or test can transfer money by forgetting to check first.");
            System.out.println("     stage-2 maps this exception to an RFC 7807 ProblemDetail automatically.");
        }

        banner("5. Repository vs DAO, and when layers stop paying rent");

        System.out.println("DAO        : 'here are SQL-ish operations' — CRUD verbs aimed at TABLES.");
        System.out.println("Repository : 'here is a collection of DOMAIN objects' — find/save/requireById,");
        System.out.println("             storage technology hidden (stage-2: Spring Data generates the impl).");
        System.out.println("trap: a service method that only forwards to the repository adds a hop, not");
        System.out.println("      value. Layers earn their keep when each OWNS something: rules, mapping, transport.");

        banner("6. In one sentence");
        System.out.println("Layered architecture is one-way traffic — requests flow down through narrower");
        System.out.println("and narrower knowledge, and only frozen DTOs make the trip back up.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
