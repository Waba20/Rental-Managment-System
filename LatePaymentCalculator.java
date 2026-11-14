// ============================================================
// FILE: LatePaymentCalculator.java
// WHAT IS THIS? A UTILITY / HELPER CLASS.
//
// This class is dedicated to one job only:
//   Calculating late payment penalties.
//
// Why a separate class?
//   The assignment specifically requires this class.
//   It also follows the "Single Responsibility Principle" —
//   a class should do ONE thing and do it well.
//
// This class does NOT need to be created as an object.
// All methods are "static" — you call them directly on the class.
//
// EXAMPLE USE:
//   double penalty = LatePaymentCalculator.calculate(dueDate, paymentDate, gracePeriod, penaltyRate);
// ============================================================

import java.time.LocalDate;         // For working with dates
import java.time.temporal.ChronoUnit; // For counting days between dates

public class LatePaymentCalculator {

    // ── METHOD: calculate(...) ───────────────────────────────
    //
    // The main calculation method.
    //
    // HOW IT WORKS:
    //   1. Add grace period days to the due date → get the "real deadline"
    //   2. If payment date is AFTER the deadline → it's late
    //   3. Count how many days LATE it was
    //   4. Multiply days late by the daily penalty rate
    //
    // Parameters:
    //   dueDate     → when rent was supposed to be paid (e.g. April 1)
    //   paymentDate → when rent was actually paid (e.g. April 10)
    //   gracePeriod → extra days allowed (e.g. 5 → deadline = April 6)
    //   penaltyRate → how much is charged PER DAY of lateness (e.g. 50.00)
    //
    // Returns: the total penalty amount (0.0 if not late)
    //
    // "static" means you don't need to create an object:
    //   LatePaymentCalculator.calculate(...) ← called directly on the class

    public static double calculate(LocalDate dueDate, LocalDate paymentDate,
                                   int gracePeriod, double penaltyRate) {

        // SAFETY CHECK: If any date is missing, no penalty possible
        if (dueDate == null || paymentDate == null) {
            return 0.0;
        }

        // STEP 1: Calculate the real deadline (due date + grace period)
        // .plusDays(gracePeriod) adds that many days to the date
        // Example: April 1 + 5 days = April 6
        LocalDate deadline = dueDate.plusDays(gracePeriod);

        // STEP 2: Check if the payment was made AFTER the deadline
        // .isAfter(deadline) returns true if paymentDate comes after deadline
        if (!paymentDate.isAfter(deadline)) {
            return 0.0; // Not late — no penalty
        }

        // STEP 3: Count how many days PAST the deadline the payment was
        // ChronoUnit.DAYS.between(start, end) counts days between two dates
        // Example: deadline = April 6, paymentDate = April 10
        //          daysLate = 4
        long daysLate = ChronoUnit.DAYS.between(deadline, paymentDate);

        // STEP 4: Calculate total penalty
        // penalty = number of days late × the daily penalty rate
        // Example: 4 days × N$50 = N$200 penalty
        double totalPenalty = daysLate * penaltyRate;

        return totalPenalty;
    }

    // ── METHOD: isLate(...) ───────────────────────────────────
    //
    // A simpler method — just tells you YES or NO: was it late?
    //
    // Returns: true if payment was after the deadline, false if not.
    //
    // This can be used independently when you just need a yes/no answer
    // and don't need the penalty amount.

    public static boolean isLate(LocalDate dueDate, LocalDate paymentDate, int gracePeriod) {
        if (dueDate == null || paymentDate == null) {
            return false; // Can't determine without dates
        }
        LocalDate deadline = dueDate.plusDays(gracePeriod);
        return paymentDate.isAfter(deadline);
    }

    // ── METHOD: getDaysLate(...)
    //
    // Returns exactly how many days late the payment was.
    // Returns 0 if it was on time or early.
    //
    // Useful for generating detailed reports like:
    //   "Tenant was 7 days late on payment #5001"

    public static long getDaysLate(LocalDate dueDate, LocalDate paymentDate, int gracePeriod) {
        if (dueDate == null || paymentDate == null) {
            return 0;
        }
        LocalDate deadline = dueDate.plusDays(gracePeriod);
        if (!paymentDate.isAfter(deadline)) {
            return 0; // On time
        }
        // ChronoUnit.DAYS.between counts days from deadline to paymentDate
        return ChronoUnit.DAYS.between(deadline, paymentDate);
    }
}

