package dio.budgeting.application;

import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class ListTransactionsByPeriodUseCase {
    private final TransactionRepository transactionRepository;
    private final ZoneId zone = ZoneId.systemDefault();

    public ListTransactionsByPeriodUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Tool(name = "list-transactions-by-period",
            description = "Lista transações financeiras criadas dentro de um período, incluindo o primeiro e o último dia. "
                    + "Use a ferramenta de data atual para resolver expressões como 'hoje' ou 'este mês'")
    public List<TransactionOutput> execute(
            @ToolParam(description = "Primeiro dia do período, no formato yyyy-MM-dd") LocalDate startDate,
            @ToolParam(description = "Último dia do período, no formato yyyy-MM-dd") LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("A data final não pode ser anterior à data inicial");
        }

        var start = startDate.atStartOfDay(zone).toInstant();
        // fim exclusivo do último dia, menos 1 ns, para incluir o dia inteiro
        var end = endDate.plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1);

        return transactionRepository.findAllByCreatedAtBetween(start, end).stream()
                .map(TransactionOutput::from)
                .toList();
    }
}
