/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


// This is the abstract base class for all property types
public abstract class Property {
    
    // Unique identifier for each property
    private int propertyId;
    
    // Physical address of the property
    private String address;
    
    // Monthly rent amount for the property
    private double rentAmount;
    
    // Boolean flag to check if property is occupied or not
    private boolean isOccupied;

    // Constructor to initialize property details
    public Property(int propertyId, String address, double rentAmount) {
        this.propertyId = propertyId;
        this.address = address;
        this.rentAmount = rentAmount;
        this.isOccupied = false; // default: property is available
    }

    // Getter method for propertyId
    public int getPropertyId() {
        return propertyId;
    }

    // Getter method for address
    public String getAddress() {
        return address;
    }

    // Getter method for rentAmount
    public double getRentAmount() {
        return rentAmount;
    }

    // Getter method for isOccupied
    public boolean isOccupied() {
        return isOccupied;
    }

    // Setter method to change occupancy status
    public void setOccupied(boolean occupied) {
        this.isOccupied = occupied;
    }
// what do we use the abstract class for
    // Abstract method to be implemented by subclasses
    // Each property type will provide its own details
    public abstract String getPropertyDetails();
} 


