package dio.budgeting.application.output;

import dio.budgeting.domain.Transaction;

import java.math.BigDecimal;

/**
 * @param value valor em reais (o domínio guarda em centavos)
 * @param createdAt data/hora em ISO-8601, ou nulo para registros antigos
 */
public record TransactionOutput(String id, String description, String category, double value, String createdAt) {
    public static TransactionOutput from(Transaction transaction) {
        return new TransactionOutput(
                transaction.getId().uuid().toString(),
                transaction.getDescription(),
                transaction.getCategory().name(),
                BigDecimal.valueOf(transaction.getAmount(), 2).doubleValue(),
                transaction.getCreatedAt() == null ? null : transaction.getCreatedAt().toString());
    }
}
