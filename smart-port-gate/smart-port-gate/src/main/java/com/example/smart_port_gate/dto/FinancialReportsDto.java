package com.example.smart_port_gate.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

public class FinancialReportsDto {

    @Data
    @Builder
    public static class BalanceSheetResponse {
        private Map<String, BigDecimal> assets;
        private BigDecimal totalAssets;
        private Map<String, BigDecimal> liabilities;
        private BigDecimal totalLiabilities;
        private BigDecimal netWorth;
    }

    @Data
    @Builder
    public static class ProfitLossResponse {
        private Map<String, BigDecimal> revenues;
        private BigDecimal totalRevenue;
        private Map<String, BigDecimal> expenses;
        private BigDecimal totalExpense;
        private BigDecimal netProfit;
    }

    @Data
    @Builder
    public static class BudgetReportResponse {
        private String gateName;
        private Integer fiscalYear;
        private BigDecimal plannedRevenue;
        private BigDecimal actualRevenue;
        private BigDecimal revenueVariance;
        private BigDecimal plannedExpense;
        private BigDecimal actualExpense;
        private BigDecimal expenseVariance;
    }
}