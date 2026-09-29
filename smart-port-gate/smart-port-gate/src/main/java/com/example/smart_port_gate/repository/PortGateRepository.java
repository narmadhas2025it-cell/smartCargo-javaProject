package com.example.smart_port_gate.repository;

import com.example.smart_port_gate.entity.PortGate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortGateRepository extends JpaRepository<PortGate, Long> {
    Optional<PortGate> findByGateCode(String gateCode);
}