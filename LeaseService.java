// ============================================================
// FILE: LeaseService.java
// WHAT IS THIS? A SERVICE CLASS (Business Logic Layer).
//
// A Service class handles the RULES and LOGIC of the system.
// It decides WHEN and HOW things can be done.
//
// Think of it like this:
//   DAO = talks to the database (store / retrieve data)
//   Service = enforces the rules (CAN we do this? SHOULD we do this?)
//
// LeaseService is responsible for:
//   - Creating new leases (with validation)
//   - Terminating leases
//   - Updating lease terms
//   - Keeping a list of all leases in memory (ArrayList)
// ============================================================

import java.time.LocalDate;   // For date values
import java.util.ArrayList;   // A resizable list (like an array that grows automatically)
import java.util.List;        // The List interface (ArrayList implements this)

public class LeaseService {

    // ── FIELD: leases ────────────────────────────────────────
    //
    // ArrayList<Payment> is a list that holds Payment objects.
    // (Payment is the concrete class that extends Lease.)
    //
    // Why ArrayList?
    //   - Regular arrays have a fixed size (array[10] = only 10 items)
    //   - ArrayList grows automatically when you add more items
    //   - We don't know in advance how many leases will exist
    //
    // "private" → only this class can access the list directly.
    // Outside classes must use the methods below.

    private ArrayList<Payment> leases;

    // ── CONSTRUCTOR ───────────────────────────────────────────
    //
    // When LeaseService is created, we initialise an empty ArrayList.
    // "new ArrayList<>()" creates a fresh empty list, ready to store leases.

    public LeaseService() {
        this.leases = new ArrayList<>();
    }

    // ── METHOD: createLease(...) ─────────────────────────────
    //
    // Creates a new lease and adds it to our list.
    //
    // This method enforces an important BUSINESS RULE:
    //   "You cannot create a lease that starts AFTER it ends."
    //
    // Parameters:
    //   All the information needed to create a Payment (which extends Lease).
    //
    // Returns: the newly created Payment object, or null if invalid.
    //
    // "null" in Java means "nothing" / "no object".
    // We return null when creation fails so the caller knows it didn't work.

    public Payment createLease(int paymentId, int leaseId,
                               double amountPaid, double amountDue,
                               LocalDate paymentDate, LocalDate dueDate,
                               int tenantId, int propertyId,
                               LocalDate startDate, LocalDate endDate,
                               double monthlyRent, double deposit,
                               int dueDay, int gracePeriod, double latePenalty) {

        // VALIDATION: Check that startDate is BEFORE endDate.
        // "startDate.isAfter(endDate)" returns true if start is after end — which is invalid.
        // "startDate.isEqual(endDate)" returns true if they are the same date — also invalid.
        if (startDate.isAfter(endDate) || startDate.isEqual(endDate)) {
            // Print an error message in red (this is a console colour code).
            System.out.println("[ERROR] Lease start date must be before end date.");
            return null; // Return nothing — lease was NOT created
        }

        // VALIDATION: Check lease ID is not already used.
        // We loop through ALL existing leases to see if the ID already exists.
        // "for (Payment p : leases)" = "for each Payment p in the leases list"
        for (Payment p : leases) {
            if (p.getLeaseID() == leaseId) {
                System.out.println("[ERROR] A lease with ID " + leaseId + " already exists.");
                return null;
            }
        }

        // All validation passed → create the new Payment object.
        // "new Payment(...)" calls the Payment constructor we wrote.
        Payment newLease = new Payment(paymentId, leaseId, amountPaid, amountDue,
                paymentDate, dueDate, tenantId, propertyId,
                startDate, endDate, monthlyRent, deposit,
                dueDay, gracePeriod, latePenalty);

        // .add(newLease) adds the new lease to the end of our ArrayList.
        leases.add(newLease);

        System.out.println("[SUCCESS] Lease #" + leaseId + " created for Tenant #" + tenantId);

        // Return the created lease so the caller can use it.
        return newLease;
    }

    // ── METHOD: terminateLease(int leaseId) ──────────────────
    //
    // Ends a lease early by finding it and calling terminate().
    //
    // We search our list for a lease with the given ID.
    // If found, we call terminate() — which is inherited from Lease.
    // terminate() sets status = "Terminated" and endDate = today.
    //
    // Returns: true if found and terminated, false if not found.

    public boolean terminateLease(int leaseId) {
        // Loop through every lease in our list.
        for (Payment p : leases) {
            // .getLeaseID() is inherited from Lease
            if (p.getLeaseID() == leaseId) {
                p.terminate(); // This method is inherited from Lease
                System.out.println("[SUCCESS] Lease #" + leaseId + " has been terminated.");
                return true; // Found and terminated
            }
        }
        System.out.println("[ERROR] Lease #" + leaseId + " not found.");
        return false; // Not found
    }

    // ── METHOD: updateMonthlyRent(int leaseId, double newRent) ──
    //
    // Allows updating the monthly rent amount on an existing lease.
    // This is the "update lease terms" requirement from the assignment.

    public boolean updateMonthlyRent(int leaseId, double newRent) {
        // VALIDATION: Rent must be a positive number.
        if (newRent <= 0) {
            System.out.println("[ERROR] Rent must be greater than zero.");
            return false;
        }
        for (Payment p : leases) {
            if (p.getLeaseID() == leaseId) {
                // setMonthlyRent() is inherited from Lease
                p.setMonthlyRent(newRent);
                System.out.println("[SUCCESS] Rent for Lease #" + leaseId
                        + " updated to N$" + newRent);
                return true;
            }
        }
        System.out.println("[ERROR] Lease #" + leaseId + " not found.");
        return false;
    }

    // ── METHOD: findLeaseById(int leaseId) ───────────────────
    //
    // Searches the list and returns the lease with the matching ID.
    // Returns null if not found.
    //
    // This is useful when the GUI wants to load a specific lease to display.

    public Payment findLeaseById(int leaseId) {
        for (Payment p : leases) {
            if (p.getLeaseID() == leaseId) {
                return p; // Found it — return this lease
            }
        }
        return null; // Not found
    }

    // ── METHOD: getAllLeases() ────────────────────────────────
    //
    // Returns the full list of all leases.
    // Useful for displaying all leases in the GUI table view.
    //
    // "List<Payment>" is the return type — we return the whole ArrayList.

    public List<Payment> getAllLeases() {
        return leases;
    }

    // ── METHOD: getActiveLeases() ────────────────────────────
    //
    // Returns ONLY the leases that are currently "Active".
    // We create a NEW list, loop through all leases,
    // and add only the active ones to the new list.
    //
    // p.isActive() is inherited from Lease — returns true if status = "Active"

    public List<Payment> getActiveLeases() {
        List<Payment> activeList = new ArrayList<>(); // Start with an empty list
        for (Payment p : leases) {
            if (p.isActive()) {           // Only add if active
                activeList.add(p);
            }
        }
        return activeList;
    }

    // ── METHOD: displayAllLeases() ───────────────────────────
    //
    // Prints the details of every lease to the console.
    // This calls Display() on each Payment object.
    // Display() is overridden in Payment — this is POLYMORPHISM.

    public void displayAllLeases() {
        if (leases.isEmpty()) {
            System.out.println("No leases found.");
            return;
        }
        System.out.println("\n===== ALL LEASES =====");
        for (Payment p : leases) {
            p.Display(); // Calls Payment's overridden Display() method
        }
    }
}
