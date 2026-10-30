package dio.budgeting.application;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionId;
import dio.budgeting.support.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionUseCasesTest {
    private InMemoryTransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
    }

    @Test
    void should_persistTransaction() {
        var useCase = new PersistTransactionUseCase(repository);

        var output = useCase.execute(new PersistTransactionInput("Mercado", 8000, Category.GROCERIES));

        assertThat(output.description()).isEqualTo("Mercado");
        assertThat(output.category()).isEqualTo("GROCERIES");
        assertThat(output.value()).isEqualTo(80.00);
        assertThat(repository.findAllByCategory(Category.GROCERIES)).hasSize(1);
    }

    @Test
    void should_listOnlyTransactionsOfTheCategory() {
        repository.save(new Transaction("Mercado", 8000, Category.GROCERIES));
        repository.save(new Transaction("Remédio", 3000, Category.PHARMA));

        var result = new ListTransactionsByCategoryUseCase(repository).execute(Category.PHARMA);

        assertThat(result).extracting("description").containsExactly("Remédio");
    }

    @Test
    void should_sumTransactionsOfTheCategory() {
        repository.save(new Transaction("Mercado 1", 8000, Category.GROCERIES));
        repository.save(new Transaction("Mercado 2", 4050, Category.GROCERIES));
        repository.save(new Transaction("Remédio", 3000, Category.PHARMA));

        var result = new SumTransactionsByCategoryUseCase(repository).execute(Category.GROCERIES);

        assertThat(result.category()).isEqualTo("GROCERIES");
        assertThat(result.transactions()).isEqualTo(2);
        assertThat(result.total()).isEqualTo(120.50);
    }

    @Test
    void should_returnZeroWhenCategoryHasNoTransactions() {
        var result = new SumTransactionsByCategoryUseCase(repository).execute(Category.LEISURE);

        assertThat(result.transactions()).isZero();
        assertThat(result.total()).isZero();
    }

    @Test
    void should_listTransactionsOfThePeriodIncludingBothEdges() {
        var zone = ZoneId.systemDefault();
        var firstDay = LocalDate.of(2026, 10, 1);
        var lastDay = LocalDate.of(2026, 10, 31);

        repository.save(at("Antes", firstDay.minusDays(1).atTime(23, 59).atZone(zone).toInstant()));
        repository.save(at("Primeiro dia", firstDay.atStartOfDay(zone).toInstant()));
        repository.save(at("Meio", LocalDate.of(2026, 10, 15).atTime(12, 0).atZone(zone).toInstant()));
        repository.save(at("Último dia", lastDay.atTime(23, 59).atZone(zone).toInstant()));
        repository.save(at("Depois", lastDay.plusDays(1).atStartOfDay(zone).toInstant()));

        var result = new ListTransactionsByPeriodUseCase(repository).execute(firstDay, lastDay);

        assertThat(result).extracting("description")
                .containsExactlyInAnyOrder("Primeiro dia", "Meio", "Último dia");
    }

    @Test
    void should_rejectPeriodWithEndBeforeStart() {
        var useCase = new ListTransactionsByPeriodUseCase(repository);

        assertThatThrownBy(() -> useCase.execute(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_returnCurrentDate() {
        var clock = Clock.fixed(Instant.parse("2026-10-30T15:00:00Z"), ZoneOffset.UTC);

        assertThat(new CurrentDateTool(clock).execute()).isEqualTo("2026-10-30");
    }

    private static Transaction at(String description, Instant createdAt) {
        return new Transaction(new TransactionId(), description, 1000, Category.OTHER, createdAt);
    }
}
