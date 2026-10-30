package dio.budgeting.application;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Modelos de linguagem não sabem a data de hoje. Esta ferramenta permite resolver
 * pedidos como "quanto gastei hoje?" ou "gastos deste mês".
 */
@Component
public class CurrentDateTool {
    private final Clock clock;

    public CurrentDateTool() {
        this(Clock.systemDefaultZone());
    }

    CurrentDateTool(Clock clock) {
        this.clock = clock;
    }

    @Tool(name = "current-date", description = "Retorna a data de hoje no formato yyyy-MM-dd")
    public String execute() {
        return LocalDate.now(clock).toString();
    }
}
