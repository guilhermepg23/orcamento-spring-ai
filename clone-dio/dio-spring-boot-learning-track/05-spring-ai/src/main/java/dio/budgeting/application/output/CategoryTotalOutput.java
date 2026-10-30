package dio.budgeting.application.output;

/**
 * @param total total gasto na categoria, em reais
 */
public record CategoryTotalOutput(String category, int transactions, double total) {
}
