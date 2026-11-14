// ============================================================
// FILE: main.java
// WHAT IS THIS? The ENTRY POINT of the program.
//
// Every Java program starts from "public static void main(String[] args)".
// This is where Java begins running your code.
//
// This file demonstrates and TESTS all your classes:
//   ✓ Lease creation with LeaseService
//   ✓ Payment recording with PaymentService
//   ✓ Late penalty calculation with LatePaymentCalculator
//   ✓ Invoice generation with InvoiceGenerator
//   ✓ Display with polymorphism (Payment's Display() method)
// ============================================================

import java.time.LocalDate; // For creating date values

public class main {

    // "public static void main(String[] args)" is always the starting point.
    //   public   = anyone can call it
    //   static   = no object needed to call it
    //   void     = returns nothing
    //   String[] args = command-line arguments (we don't use these here)

    public static void main(String[] args) {

        System.out.println("===========================================");
        System.out.println("  JAMES PROPERTY RENTAL — LEASE & PAYMENT");
        System.out.println("  Module Test by Upaheua Kauapirura");
        System.out.println("===========================================\n");

        // ── STEP 1: Create the Service objects ─────────────────
        //
        // LeaseService handles creating and managing leases.
        // PaymentService handles recording payments.
        //
        // "new LeaseService()" creates a fresh LeaseService object.
        // Java calls the constructor we wrote inside LeaseService.

        LeaseService leaseService   = new LeaseService();
        PaymentService paymentService = new PaymentService();

        // ── STEP 2: Set up dates ────────────────────────────────
        //
        // LocalDate.of(year, month, day) creates a specific date.
        // LocalDate.now() would give today's date — but we use fixed
        // dates here so the test is predictable.

        LocalDate leaseStart = LocalDate.of(2024, 1, 1);   // Jan 1, 2024
        LocalDate leaseEnd   = LocalDate.of(2025, 1, 1);   // Jan 1, 2025 (12 months)
        LocalDate dueDate    = LocalDate.of(2024, 4, 1);   // Rent due: April 1
        LocalDate paidOnTime = LocalDate.of(2024, 4, 4);   // Paid April 4 (within 5-day grace)
        LocalDate paidLate   = LocalDate.of(2024, 4, 10);  // Paid April 10 (LATE — past April 6)

        // ── STEP 3: Create Lease 1 (paid on time) ──────────────
        //
        // We pass all the information needed to createLease().
        // LeaseService validates it, then creates a Payment object.
        //
        // Arguments breakdown:
        //   5001 = paymentId, 1001 = leaseId
        //   5000.00 = amountPaid (tenant paid full rent)
        //   5000.00 = amountDue (rent owed)
        //   paidOnTime = payment date, dueDate = due date
        //   101 = tenantId, 201 = propertyId
        //   leaseStart, leaseEnd = lease period
        //   5000.00 = monthlyRent
        //   10000.00 = deposit
        //   1 = dueDay (1st of month)
        //   5 = gracePeriod (5 extra days)
        //   500.00 = latePenalty (per day penalty)

        System.out.println("--- Creating Lease #1 (ON TIME payment) ---");
        Payment lease1 = leaseService.createLease(
                5001, 1001,              // paymentId, leaseId
                5000.00, 5000.00,        // amountPaid, amountDue
                paidOnTime, dueDate,     // paymentDate, dueDate
                101, 201,                // tenantId, propertyId
                leaseStart, leaseEnd,    // startDate, endDate
                5000.00, 10000.00,       // monthlyRent, deposit
                1, 5, 500.00             // dueDay, gracePeriod, latePenalty
        );

        // If lease was created successfully (not null), display it
        if (lease1 != null) {
            lease1.Display(); // Calls Payment's overridden Display() — POLYMORPHISM
        }

        // ── STEP 4: Create Lease 2 (paid LATE) ─────────────────
        //
        // This lease uses "paidLate" (April 10) as the payment date.
        // Due date = April 1, grace period = 5 days → deadline = April 6.
        // Paid on April 10 → 4 days late → penalty should be applied.

        System.out.println("\n--- Creating Lease #2 (LATE payment) ---");
        Payment lease2 = leaseService.createLease(
                5002, 1002,              // paymentId, leaseId
                5000.00, 5000.00,        // amountPaid, amountDue
                paidLate, dueDate,       // paymentDate=April 10, dueDate=April 1
                102, 202,
                leaseStart, leaseEnd,
                5000.00, 10000.00,
                1, 5, 500.00             // penalty = 500 per day late
        );

        if (lease2 != null) {
            lease2.Display();
        }

        // ── STEP 5: Create Lease 3 (partial payment) ────────────
        //
        // Tenant paid only N$3000 but owed N$5000 → PARTIAL status.

        System.out.println("\n--- Creating Lease #3 (PARTIAL payment) ---");
        Payment lease3 = leaseService.createLease(
                5003, 1003,
                3000.00, 5000.00,        // Only paid 3000 of 5000 owed
                paidOnTime, dueDate,
                103, 203,
                leaseStart, leaseEnd,
                5000.00, 10000.00,
                1, 5, 500.00
        );

        if (lease3 != null) {
            lease3.Display();
        }

        // ── STEP 6: Test LatePaymentCalculator ──────────────────
        //
        // LatePaymentCalculator is a separate utility class.
        // All its methods are "static" so we call them on the class directly.
        // No need to do: new LatePaymentCalculator()

        System.out.println("\n--- LatePaymentCalculator Tests ---");

        // Test 1: Check if April 10 payment is late given April 1 due date + 5 grace days
        boolean isLate = LatePaymentCalculator.isLate(dueDate, paidLate, 5);
        System.out.println("Is April 10 payment late? " + isLate); // Expected: true

        // Test 2: How many days late?
        long daysLate = LatePaymentCalculator.getDaysLate(dueDate, paidLate, 5);
        System.out.println("Days late: " + daysLate); // Expected: 4 (deadline=April 6, paid April 10)

        // Test 3: Calculate penalty (4 days × N$500 per day = N$2000)
        double penalty = LatePaymentCalculator.calculate(dueDate, paidLate, 5, 500.00);
        System.out.printf("Penalty for %d days late at N$500/day: N$%.2f%n", daysLate, penalty);

        // ── STEP 7: Record a payment using PaymentService ────────
        //
        // PaymentService.recordPayment() validates and creates a payment.

        System.out.println("\n--- Recording payment via PaymentService ---");
        Payment recorded = paymentService.recordPayment(
                5004, 1001,              // New paymentId, for Lease 1001
                5000.00, 5000.00,
                paidOnTime, dueDate,
                101, 201,
                leaseStart, leaseEnd,
                5000.00, 10000.00,
                1, 5, 500.00
        );

        // ── STEP 8: Generate an invoice ──────────────────────────
        //
        // InvoiceGenerator saves a text file invoice.
        // After running this, you'll see a file "Invoice_5001.txt" in your project folder.

        if (lease1 != null) {
            System.out.println("\n--- Generating Invoice for Lease #1 ---");
            InvoiceGenerator.generate(lease1);
        }

        // ── STEP 9: Display all leases ───────────────────────────
        //
        // leaseService.displayAllLeases() loops through all leases
        // and calls Display() on each one.
        // This demonstrates ArrayList usage + Polymorphism.

        System.out.println("\n--- All Leases Summary ---");
        leaseService.displayAllLeases();

        // ── STEP 10: Test terminating a lease ────────────────────
        //
        // terminateLease() finds the lease and calls terminate() on it.
        // terminate() is inherited from Lease and sets status = "Terminated".

        System.out.println("\n--- Terminating Lease #1003 ---");
        leaseService.terminateLease(1003);

        // Show lease 3 again — status should now say "Terminated"
        Payment terminated = leaseService.findLeaseById(1003);
        if (terminated != null) {
            terminated.Display();
        }

        System.out.println("\n===========================================");
        System.out.println("  All tests completed successfully!");
        System.out.println("===========================================");
    }
}

