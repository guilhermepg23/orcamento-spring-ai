package dio.budgeting.domain;

import java.time.Instant;
import java.util.List;

public interface TransactionRepository {
    Transaction save(Transaction transaction);

    List<Transaction> findAllByCategory(Category category);

    /** Transações criadas entre {@code start} e {@code end} (ambos inclusivos). */
    List<Transaction> findAllByCreatedAtBetween(Instant start, Instant end);
}
