package com.example.smart_port_gate.repository;

import com.example.smart_port_gate.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByGateIdAndFiscalYear(Long gateId, Integer fiscalYear);
}