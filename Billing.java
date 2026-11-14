// ============================================================
// FILE: Billing.java
// WHAT IS THIS? An INTERFACE.
//
// Think of an interface like a CONTRACT or a PROMISE.
// It says: "Any class that uses me (implements me)
// MUST have these methods — no excuses."
//
// This interface is the CONTRACT for anything that handles
// billing / payments in our property rental system.
//
// WHO USES THIS? → The Payment class "implements" this interface,
// meaning Payment promises to have all these methods.
// ============================================================

public interface Billing {

    // ── METHOD 1 ──────────────────────────────────────────────
    // recordPayment(double amount)
    //
    // What it does: Records/saves a rent payment made by a tenant.
    // "void" means this method does NOT return any value.
    // "double amount" is the money the tenant paid (e.g. 5000.00)
    //
    // Example: tenant paid N$5000 → recordPayment(5000.00)
    void recordPayment(double amount);

    // ── METHOD 2 ──────────────────────────────────────────────
    // calculatePenalty()
    //
    // What it does: Works out the late fee if tenant paid after the due date.
    // "double" means this method RETURNS a number (e.g. 500.00)
    //
    // Example: rent was due on the 1st, paid on the 10th
    //          → calculatePenalty() might return 500.00
    double calculatePenalty();

    // ── METHOD 3 ──────────────────────────────────────────────
    // getOutstanding()
    //
    // What it does: Returns how much money the tenant STILL owes.
    // If tenant fully paid → returns 0.0
    // If tenant paid only part → returns what's left
    //
    // Example: rent = N$5000, tenant paid N$3000
    //          → getOutstanding() returns 2000.00
    double getOutstanding();

    // ── METHOD 4 ──────────────────────────────────────────────
    // isLate()
    //
    // What it does: Checks if the payment was made AFTER the due date.
    // "boolean" means it returns either TRUE or FALSE only.
    //
    // Example: due date = 1 April, paid on 5 April → isLate() = true
    //          due date = 1 April, paid on 31 March → isLate() = false
    boolean isLate();
}

