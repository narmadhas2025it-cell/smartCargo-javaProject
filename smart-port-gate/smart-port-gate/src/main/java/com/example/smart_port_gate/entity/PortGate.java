package com.example.smart_port_gate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "port_gates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortGate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String gateCode;

    @Column(nullable = false)
    private String zoneName; // e.g., Gate Terminal North Zone

    private Double maxWeightRatingTonnes; // e.g., 30.0 Tonnes
    private BigDecimal standardClearanceFee; // e.g., 150.00
    private BigDecimal overweightSurchargeFee; // e.g., 50.00 per excess ton or flat
}