package com.example.smart_port_gate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String poNumber;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    private Contact vendor;

    private String serviceDescription; // Scale Calibration
    private BigDecimal orderAmount;
    private String status; // CREATED, BILLED
    private LocalDateTime orderDate;
}