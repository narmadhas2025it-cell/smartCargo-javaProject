package com.example.smart_port_gate.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class GateInResponse {
    private Long entryId;
    private String containerOcrCode;
    private Double grossWeightTonnes;
    private Double maxAllowedWeightTonnes;
    private Boolean overweight;
    private BigDecimal standardFee;
    private BigDecimal surchargeFee;
    private BigDecimal totalAmount;
    private String statusMessage;
}