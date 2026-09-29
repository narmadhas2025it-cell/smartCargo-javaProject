package com.example.smart_port_gate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "gate_id", nullable = false)
    private PortGate gate;

    private Integer fiscalYear;
    private BigDecimal plannedRevenue;
    private BigDecimal plannedExpense;
}