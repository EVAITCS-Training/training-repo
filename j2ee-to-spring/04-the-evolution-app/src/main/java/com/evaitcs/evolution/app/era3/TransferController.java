package com.evaitcs.evolution.app.era3;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * ERA 3 — the hand-wired Spring MVC controller: the SAME transfer as eras 1 and 2,
 * now as JSON in / JSON out. Count what is GONE compared with TransferServlet:
 *   - no req.getParameter + parseLong/parseAmount -> @RequestBody + Jackson 3 builds
 *     the TransferRequest record (a bad body becomes a 400 in BankApiAdvice);
 *   - no resp.setContentType/getWriter/println -> RETURN a record, the message
 *     converter serializes it;
 *   - no try/catch, no setStatus -> exceptions escape to BankApiAdvice, which owns the
 *     whole error-to-HTTP table in one place;
 *   - no ServiceLocator.lookup -> constructor injection, the dependency is the signature.
 * What remains is the entire class: two methods that delegate and map DTOs.
 *
 * THE PATHS ARE RELATIVE ("/transfers", NOT "/api/v3/transfers") — and that is a
 * deliberate wiring choice, explained where the mapping is made (EvolutionServer.start)
 * and in the README walkthrough: the DispatcherServlet is mapped to the PREFIX
 * "/api/v3/*", so Tomcat strips the prefix and Spring matches on what remains
 * (servletPath="/api/v3", lookupPath="/transfers"). Boot maps its DispatcherServlet to
 * "/" instead, which is why stage-2's TransferController carries the full
 * @RequestMapping("/api/transfers") — same mechanism, different mount point.
 */
@RestController
public class TransferController {

    private final TransferService transferService;

    /** Constructor injection — the era-1 ServiceLocator lookup, dead. */
    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * POST /api/v3/transfers with {"from":2,"to":3,"amount":25.00}.
     * @RequestBody: the Jackson-3 converter deserializes the JSON into the record
     * BEFORE this method runs — inside the method, input is already typed Java.
     */
    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transfer(@RequestBody TransferRequest request) {
        TransferService.TransferReceipt receipt =
                transferService.transfer(request.from(), request.to(), request.amount());
        return ResponseEntity.status(HttpStatus.OK).body(new TransferResponse(
                receipt.reference(), receipt.newFromBalance(), receipt.newToBalance()));
    }

    /**
     * GET /api/v3/accounts/1 -> {"id":1,"type":"CHECKING","balance":1500.00}.
     * @PathVariable: "{id}" in the pattern is converted to long — era 1's
     * Long.parseLong + hand-decided 400, now framework-owned (a non-numeric id becomes
     * MethodArgumentTypeMismatchException -> 400 problem+json in the advice).
     */
    @GetMapping("/accounts/{id}")
    public AccountView account(@PathVariable("id") long id) {
        return transferService.account(id);
    }
}
