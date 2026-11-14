/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

// Tenant class represents a person renting a property
public class Tenant {
    private int tenantId;
    private String name;
    private String phone;
    private String status; // Active or Blacklisted

    // Constructor to initialize tenant details
    public Tenant(int tenantId, String name, String phone, String status) {
        this.tenantId = tenantId;
        this.name = name;
        this.phone = phone;
        this.status = status;
    }

    // Getter for tenantId
    public int getTenantId() {
        return tenantId;
    }

    // Getter for name
    public String getName() {
        return name;
    }

    // Getter for phone
    public String getPhone() {
        return phone;
    }

    // Getter for status
    public String getStatus() {
        return status;
    }

    // Setter to update tenant status (Active/Blacklisted)
    public void setStatus(String status) {
        this.status = status;
    }

    // Method to display tenant details
    public String getTenantDetails() {
        return "Tenant: " + name + " | Phone: " + phone + " | Status: " + status;
    }
}
