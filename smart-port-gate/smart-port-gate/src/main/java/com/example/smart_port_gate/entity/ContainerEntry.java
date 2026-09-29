package com.example.smart_port_gate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "container_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String containerOcrCode;

    @ManyToOne
    @JoinColumn(name = "gate_id", nullable = false)
    private PortGate gate;

    @ManyToOne
    @JoinColumn(name = "carrier_id", nullable = false)
    private Contact carrier;

    private Double grossWeightTonnes;
    private Boolean isOverweight;
    private BigDecimal calculatedTotalFee;

    private LocalDateTime entryTime;
}