package dio.budgeting.application;

import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionOutputTest {

    @Test
    void should_convertCentsToReais() {
        var output = TransactionOutput.from(new Transaction("Mercado", 8000, Category.GROCERIES));

        assertThat(output.value()).isEqualTo(80.00);
    }

    @Test
    void should_keepCentsPrecision() {
        var output = TransactionOutput.from(new Transaction("Farmácia", 1999, Category.PHARMA));

        assertThat(output.value()).isEqualTo(19.99);
    }

    @Test
    void should_acceptLegacyTransactionWithoutCreationDate() {
        var legacy = new Transaction(new TransactionId(), "Antigo", 500, Category.AUTO, null);

        assertThat(TransactionOutput.from(legacy).createdAt()).isNull();
    }

    @Test
    void should_formatCreationDateAsIso() {
        var createdAt = Instant.parse("2026-10-30T10:15:30Z");
        var tx = new Transaction(new TransactionId(), "Almoço", 4500, Category.RESTAURANT, createdAt);

        assertThat(TransactionOutput.from(tx).createdAt()).isEqualTo("2026-10-30T10:15:30Z");
    }
}
