package dev.confera.ledger.exception;

import dev.confera.shared.exception.ApiException;
import org.springframework.http.HttpStatus;

public class LedgerImbalanceException extends ApiException {

    public LedgerImbalanceException(long totalDebitCents, long totalCreditCents) {
        super("Transaction is not balanced: debit=%d cents, credit=%d cents".formatted(totalDebitCents, totalCreditCents),
            HttpStatus.BAD_REQUEST);
    }
}