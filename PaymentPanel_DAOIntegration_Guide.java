// ═══════════════════════════════════════════════════════════════════════════════
// PaymentPanel + PaymentDAO  INTEGRATION GUIDE
// ═══════════════════════════════════════════════════════════════════════════════
//
// PaymentPanel.java already handles the full UI (KPI cards, table, dialogs).
// This file shows exactly which blocks to change so every button
// calls PaymentDAO and hits SQL Server Management Studio.
//
// BUTTON → DAO MAPPING:
//   ＋ Record Payment    → PaymentDAO.insertPayment()
//   View/Edit            → PaymentDAO.getPaymentSummary()
//   Tenant Ledger        → PaymentDAO.getTotalPaidForLease()
//   (on load)            → PaymentDAO.getAllPaymentsForTable()
//
// ═══════════════════════════════════════════════════════════════════════════════
//
// STEP 1 — Add at top of PaymentPanel class:
//
//   private final PaymentDAO paymentDAO = new PaymentDAO();
//
// ═══════════════════════════════════════════════════════════════════════════════
//
// STEP 2 — Replace seedData() and refreshTable() calls in constructor with:
//
//   loadFromDB();
//
// And add this method:
//
//   private void loadFromDB() {
//       allRecords.clear();
//       tableModel.setRowCount(0);
//       try {
//           Connection conn = DBConnection.getConnection();
//           List<Object[]> rows = paymentDAO.getAllPaymentsForTable(conn);
//           // Each row: {PaymentID, TenantName, PropertyAddress, AmountPaid, PaymentDate, Status}
//           for (Object[] row : rows) {
//               tableModel.addRow(new Object[]{
//                       row[0],  // PaymentID
//                       row[1],  // TenantName
//                       row[2],  // PropertyAddress
//                       row[3],  // AmountPaid (formatted N$)
//                       row[4],  // PaymentDate (LocalDate)
//                       row[5],  // Status
//                       ""       // Actions column
//               });
//           }
//           updateKPIs();
//       } catch (SQLException e) {
//           // DB not connected: silently fall back to empty table
//           // or show an error banner
//       }
//   }
//
// ═══════════════════════════════════════════════════════════════════════════════
//
// STEP 3 — In showRecordDialog(), replace the in-memory save block:
//
//   FIND (inside confirm/save button ActionListener):
//       allRecords.add(rec);  // or allRecords.set(idx, rec) for edit
//       refreshTable(tfSearch.getText(), ...);
//
//   REPLACE WITH:
//
//       try {
//           Connection conn = DBConnection.getConnection();
//
//           // leaseID must come from the dialog's lease selector.
//           // Add a JComboBox populated from:
//           //   LeaseDAO.getAllLeasesForTable(conn)
//           // so the user picks a lease and you extract its int LeaseID.
//           int leaseID = selectedLeaseId;   // from combo
//
//           paymentDAO.insertPayment(conn, leaseID, amountPaid);
//           dialog.dispose();
//           loadFromDB();    // refresh table from DB
//           JOptionPane.showMessageDialog(this,
//               "✓  Payment recorded successfully!", "Success",
//               JOptionPane.INFORMATION_MESSAGE);
//       } catch (IllegalArgumentException ex) {
//           showError(dialog, ex.getMessage());
//       } catch (SQLException ex) {
//           showError(dialog, "Database error: " + ex.getMessage());
//       }
//
// ═══════════════════════════════════════════════════════════════════════════════
//
// STEP 4 — In showLedgerDialog(), replace in-memory balance calculation with:
//
//   try {
//       Connection conn = DBConnection.getConnection();
//       // For each active lease of the selected tenant:
//       //   paymentDAO.getPaymentSummary(conn, leaseID)  returns PaymentSummary
//       //   with fields: monthlyRent, totalPaid, balance, penaltyApplied, paymentStatus
//       PaymentDAO.PaymentSummary summary = paymentDAO.getPaymentSummary(conn, leaseID);
//       if (summary != null) {
//           lblRent.setText(String.format("N$ %.2f", summary.monthlyRent));
//           lblPaid.setText(String.format("N$ %.2f", summary.totalPaid));
//           lblBalance.setText(String.format("N$ %.2f", summary.balance));
//           lblStatus.setText(summary.paymentStatus);
//       }
//   } catch (SQLException ex) {
//       // handle
//   }
//
// ═══════════════════════════════════════════════════════════════════════════════
//
// DB TABLE REQUIRED (SQL Server Management Studio):
//
//   CREATE TABLE Payment (
//       PaymentID   INT IDENTITY PRIMARY KEY,
//       LeaseID     INT NOT NULL,
//       AmountPaid  DECIMAL(12,2) NOT NULL,
//       PaymentDate DATE NOT NULL DEFAULT GETDATE(),
//       FOREIGN KEY (LeaseID) REFERENCES Lease(LeaseID)
//   );
//
// ═══════════════════════════════════════════════════════════════════════════════
