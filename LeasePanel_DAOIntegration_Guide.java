// ═══════════════════════════════════════════════════════════════════════════════
// LeasePanel + LeaseDAO  INTEGRATION GUIDE
// ═══════════════════════════════════════════════════════════════════════════════
//
// LeasePanel.java already handles the full UI.
// This file shows exactly which 4 lines/blocks to change so every button
// calls the real LeaseDAO and hits SQL Server Management Studio.
//
// BUTTON → DAO MAPPING:
//   ＋ Create New Lease  → LeaseDAO.insertLease()     + PropertyDAO.updateStatus("Occupied")
//   Edit                 → LeaseDAO.updateLeaseTerm()
//   Terminate            → LeaseDAO.terminateLease()  + PropertyDAO.updateStatus("Available")
//   (on load)            → LeaseDAO.getAllLeasesForTable()
//
// ═══════════════════════════════════════════════════════════════════════════════
//
// STEP 1 — Add DAO fields at the top of LeasePanel class:
//
//   private final LeaseDAO    leaseDAO    = new LeaseDAO();
//   private final PropertyDAO propertyDAO = new PropertyDAO();
//
// ═══════════════════════════════════════════════════════════════════════════════
//
// STEP 2 — Replace addMockData() call in constructor with:
//
//   loadFromDB();
//
// And add this method to LeasePanel:
//
//   private void loadFromDB() {
//       model.setRowCount(0);
//       try {
//           Connection conn = DBConnection.getConnection();
//           List<Object[]> rows = leaseDAO.getAllLeasesForTable(conn);
//           // getAllLeasesForTable returns:
//           //  {LeaseID, TenantName, PropertyAddress, StartDate, EndDate,
//           //   MonthlyRent, SecurityDeposit, Status}
//           for (Object[] row : rows) {
//               // Calculate duration
//               String startStr = row[3].toString();
//               String endStr   = row[4].toString();
//               long dur = 0;
//               try {
//                   LocalDate s = LocalDate.parse(startStr);
//                   LocalDate e = LocalDate.parse(endStr);
//                   dur = ChronoUnit.DAYS.between(s, e);
//               } catch (Exception ignored) {}
//
//               model.addRow(new Object[]{
//                       row[0],                          // LeaseID  (int)
//                       row[1],                          // TenantName
//                       row[2],                          // PropertyAddress
//                       startStr,                        // StartDate
//                       endStr,                          // EndDate
//                       dur + " days",                   // Duration
//                       row[5],                          // MonthlyRent  (formatted N$)
//                       row[6],                          // SecurityDeposit (formatted N$)
//                       "—",                             // PaymentDueDate (not in summary query)
//                       "—",                             // GracePeriod
//                       "—",                             // LatePenalty
//                       row[7],                          // Status
//                       "ACTIONS"
//               });
//           }
//       } catch (SQLException e) {
//           JOptionPane.showMessageDialog(this,
//               "Could not load leases:\n" + e.getMessage(),
//               "Database Error", JOptionPane.ERROR_MESSAGE);
//       }
//   }
//
// ═══════════════════════════════════════════════════════════════════════════════
//
// STEP 3 — In openLeaseForm(), replace the in-memory save block:
//
//   FIND (inside btnSave.addActionListener):
//       if (isEdit) {
//           ...model.setValueAt(row2[i], rowToEdit, i)...
//       } else {
//           model.addRow(row2);
//       }
//       occupyPropertyByDesc(propClean, id);
//
//   REPLACE WITH:
//
//       try {
//           Connection conn = DBConnection.getConnection();
//
//           // Parse propertyID from the property combo text (e.g. "H101 - House · 34 Ind...")
//           // In a real system, store the PropertyID alongside the display string.
//           // For now, we do an in-table lookup:
//           int propertyID = lookupPropertyIDFromDB(conn, propClean);
//           int tenantID   = lookupTenantIDFromDB(conn, (String) cbTenant.getSelectedItem());
//
//           if (!isEdit) {
//               // INSERT new lease → SQL Server
//               int newLeaseID = leaseDAO.insertLease(conn,
//                       propertyID,
//                       tenantID,
//                       java.sql.Date.valueOf(start),
//                       java.sql.Date.valueOf(end),
//                       (double) rent,
//                       (double) dep,
//                       dueDay, grace, (double) pen);
//               // insertLease() automatically sets Property Status = "Occupied"
//               JOptionPane.showMessageDialog(LeasePanel.this,
//                   "Lease created. LeaseID: " + newLeaseID, "Success",
//                   JOptionPane.INFORMATION_MESSAGE);
//           } else {
//               // UPDATE existing lease → SQL Server
//               // Get the actual LeaseID from the row (column C_ID)
//               int leaseID = (Integer) model.getValueAt(rowToEdit, C_ID);
//               leaseDAO.updateLeaseTerm(conn, leaseID, rent, dep, dueDay, grace, pen,
//                       java.sql.Date.valueOf(end));
//           }
//           dlg.dispose();
//           loadFromDB();    // refresh table
//       } catch (IllegalStateException ex) {
//           // Property already occupied
//           showError(dlg, ex.getMessage());
//       } catch (IllegalArgumentException ex) {
//           showError(dlg, ex.getMessage());
//       } catch (SQLException ex) {
//           showError(dlg, "Database error: " + ex.getMessage());
//       }
//
// ═══════════════════════════════════════════════════════════════════════════════
//
// STEP 4 — In terminateLease(), replace in-memory status change:
//
//   FIND:
//       model.setValueAt("Terminated", row, C_STATUS);
//       freePropertyByDesc(model.getValueAt(row, C_PROPERTY).toString());
//
//   REPLACE WITH:
//
//       try {
//           Connection conn = DBConnection.getConnection();
//           int leaseID = (Integer) model.getValueAt(row, C_ID);
//           boolean ok = leaseDAO.terminateLease(conn, leaseID);
//           // terminateLease() automatically sets Property Status = "Available"
//           if (ok) {
//               loadFromDB();
//           } else {
//               JOptionPane.showMessageDialog(LeasePanel.this,
//                   "Lease not found or already terminated.", "Error",
//                   JOptionPane.ERROR_MESSAGE);
//           }
//       } catch (SQLException ex) {
//           JOptionPane.showMessageDialog(LeasePanel.this,
//               "Database error: " + ex.getMessage(), "Error",
//               JOptionPane.ERROR_MESSAGE);
//       }
//
// ═══════════════════════════════════════════════════════════════════════════════
//
// HELPER METHODS — add these to LeasePanel:
//
//   private int lookupPropertyIDFromDB(Connection conn, String addressOrCode)
//           throws SQLException {
//       // Try code prefix first (e.g. "H101")
//       String code = addressOrCode.split(" ")[0];
//       String sql = "SELECT PropertyID FROM Property WHERE Address LIKE ? OR PropertyID IN "
//               + "(SELECT PropertyID FROM Property WHERE CAST(PropertyID AS VARCHAR) = ?)";
//       try (PreparedStatement ps = conn.prepareStatement(
//               "SELECT PropertyID FROM Property WHERE Address LIKE ?")) {
//           ps.setString(1, "%" + addressOrCode + "%");
//           ResultSet rs = ps.executeQuery();
//           if (rs.next()) return rs.getInt(1);
//       }
//       return -1;
//   }
//
//   private int lookupTenantIDFromDB(Connection conn, String fullName)
//           throws SQLException {
//       String[] parts = fullName.trim().split(" ", 2);
//       String sql = "SELECT TenantID FROM Tenant "
//               + "WHERE FirstName = ? AND LastName = ?";
//       try (PreparedStatement ps = conn.prepareStatement(sql)) {
//           ps.setString(1, parts[0]);
//           ps.setString(2, parts.length > 1 ? parts[1] : "");
//           ResultSet rs = ps.executeQuery();
//           if (rs.next()) return rs.getInt(1);
//       }
//       return -1;
//   }
//
// ═══════════════════════════════════════════════════════════════════════════════
//
// WHY STEPS NOT AUTOMATICALLY APPLIED:
//   LeasePanel populates the tenant/property combos from static arrays (TENANTS[],
//   PROPERTIES[]). For the DAO calls to work, these combos need to be populated
//   from DB instead. The integration steps above include the correct SQL lookups.
//   Replace the static arrays with DB-loaded lists using:
//
//     TenantDAO.getAllTenantsForTable(conn)
//     PropertyDAO.getAllPropertiesForTable(conn)
//
// ═══════════════════════════════════════════════════════════════════════════════
