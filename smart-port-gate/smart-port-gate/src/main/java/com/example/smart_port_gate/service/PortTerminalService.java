package com.example.smart_port_gate.service;

import com.example.smart_port_gate.dto.FinancialReportsDto.*;
import com.example.smart_port_gate.dto.GateInRequest;
import com.example.smart_port_gate.dto.GateInResponse;
import com.example.smart_port_gate.entity.*;
import com.example.smart_port_gate.enums.AccountType;
import com.example.smart_port_gate.enums.InvoiceType;
import com.example.smart_port_gate.enums.JournalType;
import com.example.smart_port_gate.enums.PaymentStatus;
import com.example.smart_port_gate.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortTerminalService {

    private final ContactRepository contactRepo;
    private final AccountRepository accountRepo;
    private final PortGateRepository gateRepo;
    private final ContainerEntryRepository containerRepo;
    private final JournalEntryRepository journalRepo;
    private final InvoiceRepository invoiceRepo;
    private final PurchaseOrderRepository poRepo;
    private final BudgetRepository budgetRepo;

    // 1. Master Data Initializer
    @Transactional
    public void initMasterData() {
        if (accountRepo.count() > 0) return;

        // Assets
        accountRepo.save(Account.builder().code("1010").name("Gate OCR & Weighbridge Assets").type(AccountType.ASSET).balance(new BigDecimal("150000.00")).build());
        accountRepo.save(Account.builder().code("1020").name("Cash/Bank").type(AccountType.ASSET).balance(new BigDecimal("50000.00")).build());

        // Liabilities
        accountRepo.save(Account.builder().code("2010").name("Maintenance Payables").type(AccountType.LIABILITY).balance(BigDecimal.ZERO).build());
        accountRepo.save(Account.builder().code("2020").name("Security Liabilities").type(AccountType.LIABILITY).balance(BigDecimal.ZERO).build());

        // Income
        accountRepo.save(Account.builder().code("4010").name("Terminal Gate Revenue").type(AccountType.INCOME).balance(BigDecimal.ZERO).build());
        accountRepo.save(Account.builder().code("4020").name("Overweight Surcharge Revenue").type(AccountType.INCOME).balance(BigDecimal.ZERO).build());

        // Expenses
        accountRepo.save(Account.builder().code("5010").name("Weighbridge Maintenance Expenses").type(AccountType.EXPENSE).balance(BigDecimal.ZERO).build());
        accountRepo.save(Account.builder().code("5020").name("Security Operating Costs").type(AccountType.EXPENSE).balance(BigDecimal.ZERO).build());
    }

    // 2. Gate OCR & Weighbridge Ingestion
    @Transactional
    public GateInResponse processGateIn(GateInRequest req) {
        PortGate gate = gateRepo.findById(req.getGateId())
                .orElseThrow(() -> new RuntimeException("Gate not found"));
        Contact carrier = contactRepo.findById(req.getCarrierId())
                .orElseThrow(() -> new RuntimeException("Carrier not found"));

        boolean isOverweight = req.getGrossWeightTonnes() > gate.getMaxWeightRatingTonnes();
        BigDecimal standardFee = gate.getStandardClearanceFee();
        BigDecimal surchargeFee = isOverweight ? gate.getOverweightSurchargeFee() : BigDecimal.ZERO;
        BigDecimal totalAmount = standardFee.add(surchargeFee);

        ContainerEntry entry = ContainerEntry.builder()
                .containerOcrCode(req.getContainerOcrCode())
                .gate(gate)
                .carrier(carrier)
                .grossWeightTonnes(req.getGrossWeightTonnes())
                .isOverweight(isOverweight)
                .calculatedTotalFee(totalAmount)
                .entryTime(LocalDateTime.now())
                .build();
        containerRepo.save(entry);

        return GateInResponse.builder()
                .entryId(entry.getId())
                .containerOcrCode(entry.getContainerOcrCode())
                .grossWeightTonnes(req.getGrossWeightTonnes())
                .maxAllowedWeightTonnes(gate.getMaxWeightRatingTonnes())
                .overweight(isOverweight)
                .standardFee(standardFee)
                .surchargeFee(surchargeFee)
                .totalAmount(totalAmount)
                .statusMessage(isOverweight ? "Gate-in logged with Overweight Surcharge" : "Standard Gate-in logged successfully")
                .build();
    }

    // 3. Issue Customer Invoice (Double-entry Ledger: Bank/Receivable Dr, Gate/Surcharge Revenue Cr)
    @Transactional
    public Invoice issueCustomerInvoice(Long carrierId, BigDecimal standardFee, BigDecimal surchargeFee) {
        Contact carrier = contactRepo.findById(carrierId)
                .orElseThrow(() -> new RuntimeException("Carrier not found"));

        BigDecimal total = standardFee.add(surchargeFee);
        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .invoiceType(InvoiceType.CUSTOMER_INVOICE)
                .contact(carrier)
                .amount(total)
                .paymentStatus(PaymentStatus.PAID)
                .description("Container Gate Clearance & Surcharge")
                .issueDate(LocalDateTime.now())
                .build();
        invoiceRepo.save(invoice);

        Account bank = accountRepo.findByCode("1020").orElseThrow();
        Account gateRev = accountRepo.findByCode("4010").orElseThrow();

        // Dr: Bank
        bank.setBalance(bank.getBalance().add(total));
        accountRepo.save(bank);

        // Cr: Terminal Gate Revenue
        gateRev.setBalance(gateRev.getBalance().add(standardFee));
        accountRepo.save(gateRev);
        postJournal(invoice.getInvoiceNumber(), JournalType.SALES, bank, gateRev, standardFee, "Gate Clearance Fee");

        // Cr: Surcharge Revenue (if applicable)
        if (surchargeFee.compareTo(BigDecimal.ZERO) > 0) {
            Account surRev = accountRepo.findByCode("4020").orElseThrow();
            surRev.setBalance(surRev.getBalance().add(surchargeFee));
            accountRepo.save(surRev);
            postJournal(invoice.getInvoiceNumber(), JournalType.SALES, bank, surRev, surchargeFee, "Overweight Surcharge Fee");
        }

        return invoice;
    }

    // 4. Create Vendor PO for Weighbridge Calibration
    @Transactional
    public PurchaseOrder createPurchaseOrder(Long vendorId, String serviceDesc, BigDecimal amount) {
        Contact vendor = contactRepo.findById(vendorId).orElseThrow(() -> new RuntimeException("Vendor not found"));
        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber("PO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .vendor(vendor)
                .serviceDescription(serviceDesc)
                .orderAmount(amount)
                .status("CREATED")
                .orderDate(LocalDateTime.now())
                .build();
        return poRepo.save(po);
    }

    // 5. Convert PO to Vendor Bill and Pay via Bank (Dr: Expense, Cr: Bank)
    @Transactional
    public Invoice convertPOToVendorBillAndPay(Long poId) {
        PurchaseOrder po = poRepo.findById(poId).orElseThrow(() -> new RuntimeException("PO not found"));
        po.setStatus("BILLED");
        poRepo.save(po);

        Invoice bill = Invoice.builder()
                .invoiceNumber("BILL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .invoiceType(InvoiceType.VENDOR_BILL)
                .contact(po.getVendor())
                .amount(po.getOrderAmount())
                .paymentStatus(PaymentStatus.PAID)
                .description(po.getServiceDescription())
                .issueDate(LocalDateTime.now())
                .build();
        invoiceRepo.save(bill);

        Account maintenanceExpense = accountRepo.findByCode("5010").orElseThrow();
        Account bank = accountRepo.findByCode("1020").orElseThrow();

        // Dr: Expense
        maintenanceExpense.setBalance(maintenanceExpense.getBalance().add(po.getOrderAmount()));
        accountRepo.save(maintenanceExpense);

        // Cr: Bank (deduct funds)
        bank.setBalance(bank.getBalance().subtract(po.getOrderAmount()));
        accountRepo.save(bank);

        postJournal(bill.getInvoiceNumber(), JournalType.BANK, maintenanceExpense, bank, po.getOrderAmount(), "Weighbridge Maintenance Outlay");
        return bill;
    }

    private void postJournal(String ref, JournalType type, Account dr, Account cr, BigDecimal amt, String desc) {
        JournalEntry entry = JournalEntry.builder()
                .referenceNumber(ref)
                .journalType(type)
                .debitAccount(dr)
                .creditAccount(cr)
                .amount(amt)
                .description(desc)
                .entryDate(LocalDateTime.now())
                .build();
        journalRepo.save(entry);
    }

    // 6. Reports
    public BalanceSheetResponse getBalanceSheet() {
        List<Account> assets = accountRepo.findByType(AccountType.ASSET);
        List<Account> liabilities = accountRepo.findByType(AccountType.LIABILITY);

        Map<String, BigDecimal> assetMap = new HashMap<>();
        BigDecimal totalAssets = BigDecimal.ZERO;
        for (Account a : assets) {
            assetMap.put(a.getName(), a.getBalance());
            totalAssets = totalAssets.add(a.getBalance());
        }

        Map<String, BigDecimal> liabilityMap = new HashMap<>();
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        for (Account l : liabilities) {
            liabilityMap.put(l.getName(), l.getBalance());
            totalLiabilities = totalLiabilities.add(l.getBalance());
        }

        return BalanceSheetResponse.builder()
                .assets(assetMap)
                .totalAssets(totalAssets)
                .liabilities(liabilityMap)
                .totalLiabilities(totalLiabilities)
                .netWorth(totalAssets.subtract(totalLiabilities))
                .build();
    }

    public ProfitLossResponse getProfitAndLoss() {
        List<Account> incomes = accountRepo.findByType(AccountType.INCOME);
        List<Account> expenses = accountRepo.findByType(AccountType.EXPENSE);

        Map<String, BigDecimal> revMap = new HashMap<>();
        BigDecimal totalRev = BigDecimal.ZERO;
        for (Account i : incomes) {
            revMap.put(i.getName(), i.getBalance());
            totalRev = totalRev.add(i.getBalance());
        }

        Map<String, BigDecimal> expMap = new HashMap<>();
        BigDecimal totalExp = BigDecimal.ZERO;
        for (Account e : expenses) {
            expMap.put(e.getName(), e.getBalance());
            totalExp = totalExp.add(e.getBalance());
        }

        return ProfitLossResponse.builder()
                .revenues(revMap)
                .totalRevenue(totalRev)
                .expenses(expMap)
                .totalExpense(totalExp)
                .netProfit(totalRev.subtract(totalExp))
                .build();
    }

    public BudgetReportResponse getBudgetReport(Long gateId, Integer fiscalYear) {
        Budget b = budgetRepo.findByGateIdAndFiscalYear(gateId, fiscalYear)
                .orElse(Budget.builder()
                        .gate(gateRepo.findById(gateId).orElseThrow())
                        .fiscalYear(fiscalYear)
                        .plannedRevenue(BigDecimal.ZERO)
                        .plannedExpense(BigDecimal.ZERO)
                        .build());

        ProfitLossResponse pl = getProfitAndLoss();

        return BudgetReportResponse.builder()
                .gateName(b.getGate().getZoneName())
                .fiscalYear(fiscalYear)
                .plannedRevenue(b.getPlannedRevenue())
                .actualRevenue(pl.getTotalRevenue())
                .revenueVariance(pl.getTotalRevenue().subtract(b.getPlannedRevenue()))
                .plannedExpense(b.getPlannedExpense())
                .actualExpense(pl.getTotalExpense())
                .expenseVariance(b.getPlannedExpense().subtract(pl.getTotalExpense()))
                .build();
    }
}