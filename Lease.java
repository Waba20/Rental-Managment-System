// ============================================================
// FILE: Lease.java
// WHAT IS THIS? An ABSTRACT CLASS.
//
// An abstract class is like a "template" or "blueprint".
// You CANNOT create a Lease object directly (new Lease() would fail).
// Instead, other classes EXTEND (inherit from) Lease to use it.
//
// Think of it like this:
//   Lease = blueprint/template (abstract)
//   Payment = actual thing built from that blueprint (concrete)
//
// INHERITANCE: Payment extends Lease
// This means Payment gets ALL the fields and methods of Lease
// automatically — like a child inheriting traits from a parent.
// ============================================================

import java.time.LocalDate;       // Used for dates (startDate, endDate)
import java.time.temporal.ChronoUnit; // Used to calculate months between two dates

public abstract class Lease {

    // ── FIELDS (the data/information this class stores) ──────
    //
    // "protected" means:
    //   - This class (Lease) can use it ✓
    //   - Child classes (Payment) can use it ✓
    //   - Classes in other packages CANNOT use it ✗
    //
    // "private" means ONLY this class can use it directly.
    // Other classes (even children) must use getters/setters.

    protected int leaseID;          // Unique number to identify this lease (e.g. 1001)
    private int tenantID;           // Which tenant this lease belongs to (links to Tenant)
    private int propertyID;         // Which property is being rented (links to Property)
    private LocalDate startDate;    // The date the lease begins (e.g. 2024-01-01)
    private LocalDate endDate;      // The date the lease ends (e.g. 2025-01-01)
    private double monthlyRent;     // How much rent is owed every month (e.g. 5000.00)
    private double deposit;         // Security deposit paid at the start (e.g. 10000.00)
    private int dueDay;             // Which day of the month rent is due (e.g. 1 = 1st of month)
    private int gracePeriod;        // Extra days allowed before penalty kicks in (e.g. 5 days)
    private double latePenalty;     // The fee charged if rent is paid late (e.g. 500.00)
    private String status;          // Current state of the lease: "Active" or "Terminated"

    // ── CONSTRUCTOR ───────────────────────────────────────────
    //
    // A constructor is a special method that runs when you CREATE
    // a new object. It sets up all the initial values.
    //
    // "public Lease(...)" — the method name MUST match the class name.
    //
    // Parameters (the values you pass in when creating):
    //   leaseID    → the ID number for this lease
    //   tenantID   → the ID of the tenant signing the lease
    //   propertyID → the ID of the property being rented
    //   startDate  → when the lease starts
    //   endDate    → when the lease ends
    //   monthlyRent→ monthly rent amount
    //   deposit    → security deposit
    //   dueDay     → day of month rent is due
    //   gracePeriod→ days allowed before penalty
    //   latePenalty→ the penalty amount for late payment
    //   status     → initial status (will always be "Active" when created)

    public Lease(int leaseID, int tenantID, int propertyID,
                 LocalDate startDate, LocalDate endDate,
                 double monthlyRent, double deposit,
                 int dueDay, int gracePeriod, double latePenalty, String status) {

        // "this.leaseID = leaseID" means:
        //   "this" refers to the FIELD in this class
        //   "leaseID" (on the right) is the PARAMETER passed in
        // We store the passed value into the field.

        this.leaseID     = leaseID;
        this.tenantID    = tenantID;
        this.propertyID  = propertyID;
        this.startDate   = startDate;
        this.endDate     = endDate;
        this.monthlyRent = monthlyRent;
        this.deposit     = deposit;
        this.dueDay      = dueDay;
        this.gracePeriod = gracePeriod;
        this.latePenalty = latePenalty;

        // NOTE: We always set status to "Active" when creating a new lease.
        // The "status" parameter is ignored here intentionally — a brand new
        // lease should always start as Active.
        this.status = "Active";
    }

    // ── METHOD: getDurationMonths() ───────────────────────────
    //
    // Calculates how many MONTHS the lease runs for.
    // "long" return type because the number of months can be large.
    //
    // ChronoUnit.MONTHS.between(startDate, endDate)
    //   → ChronoUnit is Java's date calculation tool
    //   → .MONTHS means we want the result in months
    //   → .between(start, end) calculates the difference
    //
    // Example: start = Jan 2024, end = Jan 2025 → returns 12

    public long getDurationMonths() {
        return ChronoUnit.MONTHS.between(startDate, endDate);
    }

    // ── METHOD: isActive() ────────────────────────────────────
    //
    // Returns TRUE if the lease is currently active.
    //
    // ⚠️ IMPORTANT FIX from original code:
    // The original used (status == "Active") — this is WRONG in Java!
    // In Java, == compares memory addresses, not text content.
    // Use .equals() to compare String values properly.
    //
    // "Active".equals(status) → TRUE if status contains the word "Active"

    public boolean isActive() {
        return "Active".equals(status);
    }

    // ── METHOD: terminate() ───────────────────────────────────
    //
    // Ends the lease early (before the end date).
    // Sets status to "Terminated" and records today as the new end date.
    //
    // LocalDate.now() → gets today's date from the computer's clock.

    public void terminate() {
        this.status  = "Terminated";
        this.endDate = LocalDate.now(); // End date becomes today
    }

    // ── ABSTRACT METHOD: Display() ────────────────────────────
    //
    // "abstract" means: "I declare this method exists, but I don't
    // write the body here. Child classes MUST write the body."
    //
    // This is ABSTRACTION — we define WHAT to do (display info)
    // but not HOW to do it (each child decides for itself).
    //
    // Payment.java will override this with its own version.

    public abstract void Display();

    // ================================================================
    // GETTERS AND SETTERS
    //
    // Because our fields are "private", outside classes can't
    // access them directly. Getters and setters are the "safe door"
    // to read or change these values.
    //
    // GETTER: "get" + FieldName → returns the current value
    // SETTER: "set" + FieldName → changes the value
    // ================================================================

    // Getter for leaseID — returns the lease ID number
    public int getLeaseID() { return leaseID; }

    // Setter for leaseID — changes the lease ID number
    public void setLeaseID(int leaseID) { this.leaseID = leaseID; }

    // Getter for tenantID
    public int getTenantID() { return tenantID; }

    // Setter for tenantID
    public void setTenantID(int tenantID) { this.tenantID = tenantID; }

    // Getter for propertyID
    public int getPropertyID() { return propertyID; }

    // Setter for propertyID
    public void setPropertyID(int propertyID) { this.propertyID = propertyID; }

    // Getter for startDate — returns the lease start date
    public LocalDate getStartDate() { return startDate; }

    // Setter for startDate
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    // Getter for endDate
    public LocalDate getEndDate() { return endDate; }

    // Setter for endDate
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    // Getter for monthlyRent
    public double getMonthlyRent() { return monthlyRent; }

    // Setter for monthlyRent
    public void setMonthlyRent(double monthlyRent) { this.monthlyRent = monthlyRent; }

    // Getter for deposit
    public double getDeposit() { return deposit; }

    // Setter for deposit
    public void setDeposit(double deposit) { this.deposit = deposit; }

    // Getter for dueDay
    public int getDueDay() { return dueDay; }

    // Setter for dueDay
    public void setDueDay(int dueDay) { this.dueDay = dueDay; }

    // Getter for gracePeriod
    public int getGracePeriod() { return gracePeriod; }

    // Setter for gracePeriod
    public void setGracePeriod(int gracePeriod) { this.gracePeriod = gracePeriod; }

    // Getter for latePenalty
    public double getLatePenalty() { return latePenalty; }

    // Setter for latePenalty
    public void setLatePenalty(double latePenalty) { this.latePenalty = latePenalty; }

    // Getter for status
    public String getStatus() { return status; }

    // Setter for status
    public void setStatus(String status) { this.status = status; }
}
