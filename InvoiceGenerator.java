// ============================================================
// FILE: InvoiceGenerator.java
// WHAT IS THIS? A FILE PROCESSING CLASS.
//
// This class generates a TEXT invoice for a tenant
// and saves it as a .txt file on the computer.
//
// This satisfies the assignment requirement:
//   "Generate invoices for tenants (saved as text or PDF)"
//   "File Processing" — reading/writing files (5 marks)
//
// HOW IT WORKS:
//   1. We receive a Payment object (which has all the data)
//   2. We format the data into a nice invoice layout
//   3. We save it to a file called "Invoice_PAYMENTID.txt"
// ============================================================

import java.io.FileWriter;    // Allows us to write text to a file
import java.io.IOException;   // Exception thrown if file writing fails
import java.io.PrintWriter;   // A convenient wrapper for FileWriter — adds println()
import java.time.LocalDate;   // For getting today's date on the invoice

public class InvoiceGenerator {

    // ── METHOD: generate(Payment payment) ────────────────────
    //
    // Takes a Payment object and creates an invoice text file.
    //
    // "static" means we don't need to create an InvoiceGenerator object.
    //   Just call: InvoiceGenerator.generate(myPayment);
    //
    // The invoice file will be saved as:
    //   Invoice_12345.txt  (where 12345 is the paymentId)
    //
    // ⚠️ EXCEPTION HANDLING:
    //   File writing CAN fail (disk full, no permission, etc.)
    //   We use try-catch to handle this gracefully instead of crashing.

    public static void generate(Payment payment) {

        // Build the filename using the payment ID.
        // String.valueOf(payment.getPaymentId()) converts the int to a String.
        // "Invoice_" + "12345" + ".txt" = "Invoice_12345.txt"
        String fileName = "Invoice_" + String.valueOf(payment.getPaymentId()) + ".txt";

        // try-with-resources:
        //   The "try (...)" syntax automatically CLOSES the file
        //   when we're done — even if an error happens.
        //   This prevents "resource leaks" (files left open accidentally).
        //
        // FileWriter(fileName) opens/creates a file with that name.
        //   true = append mode (add to existing file)
        //   false/omitted = overwrite mode (replace file content)
        //
        // PrintWriter wraps FileWriter to give us println() for easy writing.

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {

            // Write the invoice content line by line.
            // writer.println("text") writes a line of text to the file.

            writer.println("============================================");
            writer.println("        JAMES PROPERTY HOLDINGS");
            writer.println("             RENT INVOICE");
            writer.println("============================================");

            // LocalDate.now() gets today's date from the system clock.
            writer.println("Invoice Date  : " + LocalDate.now());
            writer.println("Invoice #     : " + payment.getPaymentId());
            writer.println("--------------------------------------------");
            writer.println("TENANT DETAILS:");

            // getTenantID() is inherited from Lease (via Payment)
            writer.println("  Tenant ID   : " + payment.getTenantID());

            // getPropertyID() is also inherited from Lease
            writer.println("  Property ID : " + payment.getPropertyID());

            // getLeaseID() is inherited from Lease
            writer.println("  Lease ID    : " + payment.getLeaseID());
            writer.println("--------------------------------------------");
            writer.println("PAYMENT DETAILS:");

            // String.format("%.2f", value) formats a number with 2 decimal places
            // e.g. 5000.0 → "5000.00"
            writer.println("  Amount Due  : N$" + String.format("%.2f", payment.getAmountDue()));
            writer.println("  Amount Paid : N$" + String.format("%.2f", payment.getAmountPaid()));
            writer.println("  Penalty     : N$" + String.format("%.2f", payment.getPenaltyApplied()));
            writer.println("  Outstanding : N$" + String.format("%.2f", payment.getOutstandingBalance()));
            writer.println("  Due Date    : " + payment.getDueDate());
            writer.println("  Paid On     : " + payment.getPaymentDate());
            writer.println("--------------------------------------------");
            writer.println("  STATUS      : " + payment.getPaymentStatus());
            writer.println("============================================");

            // Check if there's an outstanding balance and add a reminder
            if (payment.getOutstandingBalance() > 0) {
                writer.println("NOTICE: You have an outstanding balance of N$"
                        + String.format("%.2f", payment.getOutstandingBalance()));
                writer.println("Please settle this amount as soon as possible.");
                writer.println("============================================");
            }

            // Print confirmation to the CONSOLE (not the file)
            System.out.println("[SUCCESS] Invoice saved to: " + fileName);

        } catch (IOException e) {
            // IOException is thrown when file reading/writing fails.
            // Instead of crashing the whole program, we print an error message.
            //
            // e.getMessage() gives a description of what went wrong.
            System.out.println("[ERROR] Could not generate invoice: " + e.getMessage());
        }
    }

    // ── METHOD: generateSummary(Payment[] payments) ──────────
    //
    // Generates a summary invoice showing MULTIPLE payments
    // for the same tenant in one file.
    //
    // "Payment[] payments" is an ARRAY of Payment objects.
    // Arrays have a fixed size, unlike ArrayList.
    // This demonstrates the use of arrays (required by assignment).
    //
    // "tenantId" — we include this so the filename is per-tenant.

    public static void generateSummary(Payment[] payments, int tenantId) {

        // No payments? Nothing to write.
        if (payments == null || payments.length == 0) {
            System.out.println("[ERROR] No payments provided for summary.");
            return;
        }

        String fileName = "Summary_Tenant_" + tenantId + ".txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {

            writer.println("============================================");
            writer.println("     JAMES PROPERTY HOLDINGS");
            writer.println("       PAYMENT SUMMARY REPORT");
            writer.println("============================================");
            writer.println("Tenant ID     : " + tenantId);
            writer.println("Report Date   : " + LocalDate.now());
            writer.println("--------------------------------------------");

            double grandTotal = 0.0; // Running total of all payments

            // Loop through each payment using a standard for loop with index.
            // "i" starts at 0 (arrays start at index 0 in Java).
            // "payments.length" is the total count of items in the array.
            // "i++" means increase i by 1 after each loop.
            for (int i = 0; i < payments.length; i++) {
                Payment p = payments[i]; // Get the payment at position i

                // "i + 1" so we show 1, 2, 3... instead of 0, 1, 2...
                writer.println("Payment " + (i + 1) + ":");
                writer.println("  Paid: N$" + String.format("%.2f", p.getAmountPaid())
                        + "  |  Status: " + p.getPaymentStatus()
                        + "  |  Date: " + p.getPaymentDate());

                grandTotal += p.getAmountPaid(); // Add to running total
            }

            writer.println("--------------------------------------------");
            writer.printf("TOTAL PAID: N$%.2f%n", grandTotal);
            writer.println("============================================");

            System.out.println("[SUCCESS] Summary saved to: " + fileName);

        } catch (IOException e) {
            System.out.println("[ERROR] Could not generate summary: " + e.getMessage());
        }
    }
}
