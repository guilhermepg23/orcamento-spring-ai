package dio.budgeting.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class Transaction {
    private TransactionId id;
    private String description;
    /** Valor em centavos. */
    private long amount;
    private Category category;
    private Instant createdAt;

    public Transaction(String description, long amount, Category category) {
        this(new TransactionId(), description, amount, category, Instant.now());
    }
}
