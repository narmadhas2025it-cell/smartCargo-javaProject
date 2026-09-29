package com.example.smart_port_gate.dto;

import lombok.Data;

@Data
public class GateInRequest {
    private String containerOcrCode;
    private Long gateId;
    private Long carrierId;
    private Double grossWeightTonnes;
}