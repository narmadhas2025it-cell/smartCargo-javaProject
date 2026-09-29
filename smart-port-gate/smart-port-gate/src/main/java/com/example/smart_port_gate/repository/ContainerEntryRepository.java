package com.example.smart_port_gate.repository;

import com.example.smart_port_gate.entity.ContainerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContainerEntryRepository extends JpaRepository<ContainerEntry, Long> {
    List<ContainerEntry> findByGateId(Long gateId);
}