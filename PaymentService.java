// ============================================================
// FILE: PaymentService.java
// WHAT IS THIS? A SERVICE CLASS for Payment operations.
//
// PaymentService handles the RULES around payments:
//   - Recording a new payment
//   - Applying late penalties automatically
//   - Checking outstanding balances
//   - Keeping a list of all payments made
//
// This is the BUSINESS LOGIC layer — it decides HOW and WHEN
// payments are processed, and enforces all the rules.
// ============================================================

import java.time.LocalDate;  // For dates
import java.util.ArrayList;  // Resizable list
import java.util.List;       // List interface

public class PaymentService {

    // ── FIELD: payments ──────────────────────────────────────
    //
    // We keep ALL payments in this ArrayList so we can look them
    // up, display them, and generate reports from them.
    //
    // This is the DATA STRUCTURE requirement from the assignment.

    private ArrayList<Payment> payments;

    // ── CONSTRUCTOR ───────────────────────────────────────────

    public PaymentService() {
        this.payments = new ArrayList<>();
    }

    // ── METHOD: recordPayment(...) ───────────────────────────
    //
    // The main method for recording a rent payment.
    //
    // WHAT IT DOES:
    //   1. Validates the amount (can't be negative or zero)
    //   2. Creates a new Payment object
    //   3. Automatically calculates penalty and outstanding
    //   4. Adds it to the payments list
    //   5. Returns the Payment object
    //
    // This satisfies the assignment requirement:
    //   "Record rent payments, partial payments, assign status"

    public Payment recordPayment(int paymentId, int leaseId,
                                 double amountPaid, double amountDue,
                                 LocalDate paymentDate, LocalDate dueDate,
                                 int tenantId, int propertyId,
                                 LocalDate startDate, LocalDate endDate,
                                 double monthlyRent, double deposit,
                                 int dueDay, int gracePeriod, double latePenalty) {

        // VALIDATION: Amount paid must be a positive number
        // A payment of 0 or negative doesn't make sense
        if (amountPaid < 0) {
            System.out.println("[ERROR] Payment amount cannot be negative.");
            return null;
        }

        // VALIDATION: Amount due must be positive
        if (amountDue <= 0) {
            System.out.println("[ERROR] Amount due must be greater than zero.");
            return null;
        }

        // VALIDATION: Payment date must not be null
        if (paymentDate == null) {
            System.out.println("[ERROR] Payment date is required.");
            return null;
        }

        // VALIDATION: Due date must not be null
        if (dueDate == null) {
            System.out.println("[ERROR] Due date is required.");
            return null;
        }

        // All validations passed — create the Payment object.
        // The Payment constructor automatically:
        //   1. Calls calculatePenalty() to check if late
        //   2. Calls getOutstanding() to find remaining balance
        //   3. Calls determineStatus() to set PAID/LATE/PARTIAL/PENDING
        Payment payment = new Payment(paymentId, leaseId, amountPaid, amountDue,
                paymentDate, dueDate, tenantId, propertyId,
                startDate, endDate, monthlyRent, deposit,
                dueDay, gracePeriod, latePenalty);

        // Add this payment to our in-memory list
        payments.add(payment);

        // Print a confirmation to the console
        System.out.println("[PAYMENT RECORDED]");
        System.out.printf("  Tenant #%d paid N$%.2f for Lease #%d | Status: %s%n",
                tenantId, amountPaid, leaseId, payment.getPaymentStatus());

        // If a penalty was applied, notify the user
        if (payment.getPenaltyApplied() > 0) {
            System.out.printf("  ⚠ Late penalty of N$%.2f applied.%n",
                    payment.getPenaltyApplied());
        }

        // If something is still owed, show the outstanding amount
        if (payment.getOutstandingBalance() > 0) {
            System.out.printf("  ℹ Outstanding balance: N$%.2f%n",
                    payment.getOutstandingBalance());
        }

        return payment;
    }

    // ── METHOD: getPaymentsForLease(int leaseId) ─────────────
    //
    // Returns ALL payments made for a specific lease.
    // Useful when a tenant wants to see their full payment history.
    //
    // We loop through all payments and only return those
    // where the leaseID matches the one we're looking for.

    public List<Payment> getPaymentsForLease(int leaseId) {
        List<Payment> result = new ArrayList<>();
        for (Payment p : payments) {
            if (p.getLeaseID() == leaseId) { // getLeaseID() is from Lease (inherited)
                result.add(p);
            }
        }
        return result;
    }

    // ── METHOD: getTotalPaidForLease(int leaseId) ────────────
    //
    // Adds up all payments made for a specific lease.
    // Useful for showing total rent collected for a property.
    //
    // We use a "running total" (sum) approach:
    //   Start at 0, add each payment amount, return the total.

    public double getTotalPaidForLease(int leaseId) {
        double total = 0.0; // Start at zero
        for (Payment p : payments) {
            if (p.getLeaseID() == leaseId) {
                total += p.getAmountPaid(); // "+=" means: total = total + amountPaid
            }
        }
        return total;
    }

    // ── METHOD: hasOutstandingBalance(int leaseId) ───────────
    //
    // Checks if a tenant still owes money for a specific lease.
    // Returns true if ANY payment for this lease has outstanding > 0.
    //
    // Useful for blacklisting tenants or blocking new leases.

    public boolean hasOutstandingBalance(int leaseId) {
        for (Payment p : payments) {
            if (p.getLeaseID() == leaseId && p.getOutstandingBalance() > 0) {
                return true; // Found at least one unpaid balance
            }
        }
        return false; // All payments are settled
    }

    // ── METHOD: getLatePayments() ────────────────────────────
    //
    // Returns all payments that were made LATE.
    // Useful for generating the "overdue payments" report.

    public List<Payment> getLatePayments() {
        List<Payment> lateList = new ArrayList<>();
        for (Payment p : payments) {
            // getPaymentStatus() returns "LATE" or "PARTIAL" etc.
            // We also include PARTIAL because those are still outstanding
            if ("LATE".equals(p.getPaymentStatus()) ||
                    "PARTIAL".equals(p.getPaymentStatus())) {
                lateList.add(p);
            }
        }
        return lateList;
    }

    // ── METHOD: getPendingPayments() ─────────────────────────
    //
    // Returns all payments with status "PENDING" — nothing paid yet.
    // Useful for chasing tenants who haven't paid at all.

    public List<Payment> getPendingPayments() {
        List<Payment> pendingList = new ArrayList<>();
        for (Payment p : payments) {
            if ("PENDING".equals(p.getPaymentStatus())) {
                pendingList.add(p);
            }
        }
        return pendingList;
    }

    // ── METHOD: getAllPayments() ──────────────────────────────
    //
    // Returns the full list of all payments.
    // Used by the GUI to show the full payment history table.

    public List<Payment> getAllPayments() {
        return payments;
    }

    // ── METHOD: displayPaymentsForLease(int leaseId) ─────────
    //
    // Prints all payment details for a specific lease to the console.
    // Uses the overridden Display() method on each Payment object.
    // This demonstrates POLYMORPHISM.

    public void displayPaymentsForLease(int leaseId) {
        List<Payment> leasePayments = getPaymentsForLease(leaseId);
        if (leasePayments.isEmpty()) {
            System.out.println("No payments found for Lease #" + leaseId);
            return;
        }
        System.out.println("\n===== PAYMENTS FOR LEASE #" + leaseId + " =====");
        for (Payment p : leasePayments) {
            p.Display(); // Polymorphism: calls Payment's version of Display()
        }
        System.out.printf("TOTAL PAID: N$%.2f%n", getTotalPaidForLease(leaseId));
    }
}
