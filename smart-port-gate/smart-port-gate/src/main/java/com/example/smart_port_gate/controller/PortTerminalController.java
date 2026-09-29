package com.example.smart_port_gate.controller;

import com.example.smart_port_gate.dto.FinancialReportsDto.*;
import com.example.smart_port_gate.dto.GateInRequest;
import com.example.smart_port_gate.dto.GateInResponse;
import com.example.smart_port_gate.entity.*;
import com.example.smart_port_gate.repository.BudgetRepository;
import com.example.smart_port_gate.repository.ContactRepository;
import com.example.smart_port_gate.repository.PortGateRepository;
import com.example.smart_port_gate.service.PortTerminalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/port")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PortTerminalController {

    private final PortTerminalService portService;
    private final PortGateRepository gateRepo;
    private final ContactRepository contactRepo;
    private final BudgetRepository budgetRepo;

    // 1. Initialize Chart of Accounts
    @PostMapping("/init-coa")
    public ResponseEntity<String> initChartOfAccounts() {
        portService.initMasterData();
        return ResponseEntity.ok("Chart of Accounts initialized successfully.");
    }

    // 2. Register Gate Terminal (POST /api/port/gates)
    @PostMapping("/gates")
    public ResponseEntity<PortGate> createGate(@RequestBody PortGate gate) {
        return ResponseEntity.ok(gateRepo.save(gate));
    }

    // 3. Register Shipping Lines / Vendors
    @PostMapping("/contacts")
    public ResponseEntity<Contact> createContact(@RequestBody Contact contact) {
        return ResponseEntity.ok(contactRepo.save(contact));
    }

    // 4. Ingest Gate Scan (POST /api/port/gate-in)
    @PostMapping("/gate-in")
    public ResponseEntity<GateInResponse> processGateIn(@RequestBody GateInRequest req) {
        return ResponseEntity.ok(portService.processGateIn(req));
    }

    // 5. Issue Customer Invoice
    @PostMapping("/invoices/customer")
    public ResponseEntity<Invoice> issueCustomerInvoice(
            @RequestParam Long carrierId,
            @RequestParam BigDecimal standardFee,
            @RequestParam BigDecimal surchargeFee) {
        return ResponseEntity.ok(portService.issueCustomerInvoice(carrierId, standardFee, surchargeFee));
    }

    // 6. Create PO & Vendor Bill Payment Flow
    @PostMapping("/purchase-orders")
    public ResponseEntity<PurchaseOrder> createPO(
            @RequestParam Long vendorId,
            @RequestParam String description,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(portService.createPurchaseOrder(vendorId, description, amount));
    }

    @PostMapping("/purchase-orders/{id}/pay")
    public ResponseEntity<Invoice> payVendorBill(@PathVariable Long id) {
        return ResponseEntity.ok(portService.convertPOToVendorBillAndPay(id));
    }

    // 7. Setup Budget
    @PostMapping("/budgets")
    public ResponseEntity<Budget> setupBudget(@RequestBody Budget budget) {
        return ResponseEntity.ok(budgetRepo.save(budget));
    }

    // 8. Financial & Analytical Reports
    @GetMapping("/reports/balance-sheet")
    public ResponseEntity<BalanceSheetResponse> getBalanceSheet() {
        return ResponseEntity.ok(portService.getBalanceSheet());
    }

    @GetMapping("/reports/profit-and-loss")
    public ResponseEntity<ProfitLossResponse> getProfitLoss() {
        return ResponseEntity.ok(portService.getProfitAndLoss());
    }

    @GetMapping("/reports/budget")
    public ResponseEntity<BudgetReportResponse> getBudgetReport(
            @RequestParam Long gateId,
            @RequestParam Integer fiscalYear) {
        return ResponseEntity.ok(portService.getBudgetReport(gateId, fiscalYear));
    }
}