package dio.budgeting.support;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Repositório falso para testar casos de uso sem banco de dados. */
public class InMemoryTransactionRepository implements TransactionRepository {
    private final List<Transaction> transactions = new ArrayList<>();

    @Override
    public Transaction save(Transaction transaction) {
        transactions.add(transaction);
        return transaction;
    }

    @Override
    public List<Transaction> findAllByCategory(Category category) {
        return transactions.stream().filter(t -> t.getCategory() == category).toList();
    }

    @Override
    public List<Transaction> findAllByCreatedAtBetween(Instant start, Instant end) {
        return transactions.stream()
                .filter(t -> t.getCreatedAt() != null
                        && !t.getCreatedAt().isBefore(start)
                        && !t.getCreatedAt().isAfter(end))
                .toList();
    }
}
