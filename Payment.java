// ============================================================
// FILE: Payment.java
// WHAT IS THIS? A CONCRETE CLASS (can be used to create objects).
//
// Payment EXTENDS Lease → it INHERITS everything from Lease.
//   Meaning: Payment automatically has leaseID, tenantID,
//   startDate, endDate, monthlyRent, etc. from Lease.
//
// Payment IMPLEMENTS Billing → it FULFILLS the Billing contract.
//   Meaning: Payment MUST have recordPayment(), calculatePenalty(),
//   getOutstanding(), and isLate() methods.
//
// INHERITANCE CHAIN:
//   Lease (abstract parent)
//     └── Payment (child — extends Lease, implements Billing)
//
// OOP CONCEPTS SHOWN HERE:
//   ✓ Inheritance    (extends Lease)
//   ✓ Abstraction    (implements Billing interface)
//   ✓ Encapsulation  (private fields + getters/setters)
//   ✓ Polymorphism   (overrides Display() from Lease)
// ============================================================

import java.time.LocalDate; // Java class for working with dates (year-month-day)

public class Payment extends Lease implements Billing {

    // ── FIELDS specific to Payment (not in Lease) ────────────
    //
    // These are the extra pieces of information a Payment needs
    // beyond what Lease already stores.

    private int    paymentId;       // Unique ID for this payment record (e.g. 5001)
    private double amountPaid;      // How much the tenant actually paid (e.g. 5000.00)
    private double amountDue;       // How much was owed for this period (e.g. 5000.00)
    private LocalDate paymentDate;  // The date the tenant actually paid (e.g. 2024-04-05)
    private LocalDate dueDate;      // The date payment was supposed to be made (e.g. 2024-04-01)
    private double penaltyApplied;  // The late fee that was charged (0.0 if paid on time)
    private double outstanding;     // How much is still owed after this payment (0.0 if fully paid)
    private String paymentStatus;   // "PAID", "LATE", "PENDING", or "PARTIAL"

    // ── CONSTRUCTOR ───────────────────────────────────────────
    //
    // This constructor takes ALL the information needed to create
    // a full Payment record.
    //
    // "super(...)" — this calls the PARENT class (Lease) constructor.
    // Because Payment extends Lease, we must set up Lease's data first.
    // "super" is the way we pass values up to the parent.
    //
    // Parameters explained:
    //   paymentId    → ID for this specific payment
    //   leaseId      → which lease this payment is for
    //   amountPaid   → money the tenant paid right now
    //   amountDue    → money that was owed
    //   paymentDate  → when tenant paid
    //   dueDate      → when tenant was supposed to pay
    //   tenantId     → who is paying
    //   propertyId   → which property
    //   startDate    → lease start
    //   endDate      → lease end
    //   monthlyRent  → rent per month
    //   deposit      → security deposit
    //   dueDay       → day of month rent is due
    //   gracePeriod  → extra days before penalty
    //   latePenalty  → amount of penalty for late payment

    public Payment(int paymentId, int leaseId,
                   double amountPaid, double amountDue,
                   LocalDate paymentDate, LocalDate dueDate,
                   int tenantId, int propertyId,
                   LocalDate startDate, LocalDate endDate,
                   double monthlyRent, double deposit,
                   int dueDay, int gracePeriod, double latePenalty) {

        // super(...) calls the Lease constructor with the lease-related data.
        // We pass "" as status because Lease always sets it to "Active" internally.
        super(leaseId, tenantId, propertyId, startDate, endDate,
                monthlyRent, deposit, dueDay, gracePeriod, latePenalty, "Active");

        // Now set the Payment-specific fields using the parameters passed in.
        this.paymentId   = paymentId;
        this.amountPaid  = amountPaid;
        this.amountDue   = amountDue;
        this.paymentDate = paymentDate;   // ✅ FIX: original code forgot to assign these!
        this.dueDate     = dueDate;       // ✅ FIX: original code forgot to assign these!

        // Now AUTO-CALCULATE the penalty, outstanding, and status.
        // These are calculated AFTER we set paymentDate and dueDate above
        // because the calculations need those values.

        // calculatePenalty() checks if paymentDate is after dueDate.
        // If late → returns the latePenalty amount. If on time → returns 0.
        this.penaltyApplied = calculatePenalty();

        // getOutstanding() calculates: (amountDue + penalty) - amountPaid
        this.outstanding    = getOutstanding();

        // determineStatus() figures out if payment is PAID/LATE/PARTIAL/PENDING
        this.paymentStatus  = determineStatus();
    }

    // ── METHOD: recordPayment(double amount) ──────────────────
    // Implements the Billing interface method.
    //
    // This is called when a tenant makes a payment.
    // It updates amountPaid, recalculates outstanding balance,
    // and updates the status.
    //
    // "@Override" tells Java: "This method replaces the one
    // declared in the Billing interface."
    //
    // Example: tenant owes N$5000, calls recordPayment(3000)
    //          → amountPaid = 3000, outstanding = 2000, status = "PARTIAL"

    @Override
    public void recordPayment(double amount) {
        this.amountPaid   = amount;              // Update how much was paid
        this.outstanding  = getOutstanding();    // Recalculate what's still owed
        this.paymentStatus = determineStatus();  // Update the status label
    }

    // ── METHOD: calculatePenalty() ────────────────────────────
    // Implements the Billing interface method.
    //
    // Checks if the payment was late. If yes, returns the penalty amount.
    // If no, returns 0.00 (no penalty).
    //
    // isLate() is our own method defined below — it returns true/false.
    // getLatePenalty() is inherited from Lease — it returns the penalty amount.
    //
    // Example:
    //   - dueDate = April 1, paymentDate = April 10 → isLate() = true
    //   - getLatePenalty() = 500.00
    //   - calculatePenalty() returns 500.00

    @Override
    public double calculatePenalty() {
        if (isLate()) {
            return getLatePenalty(); // getLatePenalty() is inherited from Lease
        }
        return 0.00; // No penalty if paid on time
    }

    // ── METHOD: getOutstanding() ──────────────────────────────
    // Implements the Billing interface method.
    //
    // Calculates how much money is still owed after the payment.
    //
    // Formula:
    //   total = amountDue + penaltyApplied
    //   outstanding = total - amountPaid
    //   (but can't go below 0 — you can't owe negative money)
    //
    // Math.max(0, diff) → returns the LARGER of 0 or diff.
    //   If diff = -100 (overpaid), Math.max(0, -100) = 0
    //   If diff =  500 (underpaid), Math.max(0, 500) = 500
    //
    // Example:
    //   amountDue = 5000, penalty = 500, amountPaid = 3000
    //   total = 5500, diff = 5500 - 3000 = 2500 → outstanding = 2500

    @Override
    public double getOutstanding() {
        double total = amountDue + penaltyApplied;  // Total owed = rent + any penalty
        double diff  = total - amountPaid;           // Subtract what was actually paid
        return Math.max(0, diff);                    // Cannot be negative
    }

    // ── METHOD: isLate() ──────────────────────────────────────
    // Implements the Billing interface method.
    //
    // Returns TRUE if the tenant paid AFTER the due date (considering grace period).
    //
    // Grace period = extra days allowed before penalty.
    // e.g. dueDay = 1st of month, gracePeriod = 5 → tenant can pay up to 5th without penalty
    //
    // dueDate.plusDays(gracePeriod) → adds grace period days to the due date
    // paymentDate.isAfter(deadline) → TRUE if tenant paid after the deadline
    //
    // We also check: paymentDate != null
    // This protects against a crash if paymentDate was never set.
    //
    // Example:
    //   dueDate = April 1, gracePeriod = 5, paymentDate = April 7
    //   deadline = April 6
    //   isLate() = true (paid on 7th, deadline was 6th)

    @Override
    public boolean isLate() {
        if (paymentDate == null || dueDate == null) {
            return false; // Can't determine if late without dates — assume not late
        }
        // Add the grace period to the due date to get the real deadline
        LocalDate deadline = dueDate.plusDays(getGracePeriod());
        // If paymentDate is AFTER the deadline → late
        return paymentDate.isAfter(deadline);
    }

    // ── PRIVATE METHOD: determineStatus() ────────────────────
    // A helper method only used inside this class (hence "private").
    //
    // This method reads the payment situation and returns the right label.
    //
    // Logic (in order):
    //   1. If nothing was paid yet           → "PENDING"
    //   2. If something is still owed        → "PARTIAL"
    //   3. If fully paid but was late        → "LATE"
    //   4. If fully paid and on time         → "PAID"
    //
    // We check in this ORDER because:
    //   - PENDING has priority (paid nothing)
    //   - PARTIAL means some was paid
    //   - LATE means fully paid but after the deadline
    //   - PAID is the best case scenario

    private String determineStatus() {
        if (amountPaid <= 0)          return "PENDING";  // No payment made at all
        if (getOutstanding() > 0)     return "PARTIAL";  // Some paid, but not all
        if (isLate())                 return "LATE";     // Fully paid, but too late
        return "PAID";                                    // Fully paid, on time
    }

    // ── METHOD: Display() ─────────────────────────────────────
    // POLYMORPHISM: This method overrides the abstract Display()
    // from Lease. Each class can show its info differently.
    //
    // @Override confirms we're replacing the parent's version.
    //
    // System.out.printf() → prints formatted text.
    //   %d = integer number
    //   %.2f = decimal number with 2 decimal places
    //   %s = string (text)
    //   %n = new line
    //
    // getLeaseID() is inherited from Lease — we call it here because
    // leaseID is "protected" in Lease.

    @Override
    public void Display() {
        System.out.println("========================================");
        System.out.printf("  Payment ID   : %d%n",    paymentId);
        System.out.printf("  Lease ID     : %d%n",    getLeaseID());
        System.out.printf("  Tenant ID    : %d%n",    getTenantID());
        System.out.printf("  Property ID  : %d%n",    getPropertyID());
        System.out.printf("  Amount Due   : N$%.2f%n", amountDue);
        System.out.printf("  Amount Paid  : N$%.2f%n", amountPaid);
        System.out.printf("  Penalty      : N$%.2f%n", penaltyApplied);
        System.out.printf("  Outstanding  : N$%.2f%n", outstanding);
        System.out.printf("  Payment Date : %s%n",    paymentDate);
        System.out.printf("  Due Date     : %s%n",    dueDate);
        System.out.printf("  Status       : %s%n",    paymentStatus);
        System.out.println("========================================");
    }

    // ================================================================
    // GETTERS AND SETTERS
    //
    // All fields are private, so we need these to safely read/write them.
    // This is ENCAPSULATION — hiding the data and providing controlled access.
    // ================================================================

    public int getPaymentId()                   { return paymentId; }
    public void setPaymentId(int id)             { this.paymentId = id; }

    // Note: getLeaseID() is already provided by Lease (inherited).
    // We provide setLeaseId here as a convenience that updates leaseID via Lease's field.
    public void setLeaseId(int id)               { this.leaseID = id; }

    public double getAmountPaid()               { return amountPaid; }
    public void setAmountPaid(double a) {
        this.amountPaid = a;
        // When amount paid changes, recalculate outstanding and status
        this.outstanding   = getOutstanding();
        this.paymentStatus = determineStatus();
    }

    public double getAmountDue()                { return amountDue; }
    public void setAmountDue(double a)           { this.amountDue = a; }

    public LocalDate getPaymentDate()           { return paymentDate; }
    public void setPaymentDate(LocalDate d) {
        this.paymentDate    = d;
        // When payment date changes, recalculate penalty and status
        this.penaltyApplied = calculatePenalty();
        this.paymentStatus  = determineStatus();
    }

    public LocalDate getDueDate()               { return dueDate; }
    public void setDueDate(LocalDate d)          { this.dueDate = d; }

    public double getPenaltyApplied()           { return penaltyApplied; }
    public void setPenaltyApplied(double p)      { this.penaltyApplied = p; }

    public double getOutstandingBalance()       { return outstanding; }
    public void setOutstanding(double o)         { this.outstanding = o; }

    public String getPaymentStatus()            { return paymentStatus; }
    public void setPaymentStatus(String s)       { this.paymentStatus = s; }
}
