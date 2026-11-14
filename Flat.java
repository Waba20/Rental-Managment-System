/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author tjiro
 */

// Declares the Flat class, which EXTENDS (inherits from) the Property class.
// This means Flat IS-A Property and gains all of Property's fields and methods.
public class Flat extends Property {

    // PRIVATE INSTANCE VARIABLES (Fields)
    // These store the unique attributes of a Flat that other property types don't have.
    // 'private' means these can ONLY be accessed within this class — this is ENCAPSULATION.

    private int floorNumber;      // Which floor the flat is located on (e.g., 1, 2, 3)
    private String unitNumber;    // The specific unit identifier (e.g., "A", "101", "B3")
    private double floorSize;     // Size of the flat in square meters (e.g., 85.5)
    private boolean elevatorAccess; // true if the building has elevator access to this flat, false otherwise

    // CONSTRUCTOR
    // This special method runs when you create a new Flat object using 'new Flat(...)'.
    // It initializes ALL fields (both inherited from Property and new ones specific to Flat).
    public Flat(int propertyId, String address, double rentAmount,
                int floorNumber, String unitNumber, double floorSize, boolean elevatorAccess) {

        // 'super()' calls the CONSTRUCTOR of the PARENT class (Property).
        // It passes the common attributes (propertyId, address, rentAmount) UP to Property
        // so Property can store them in its own fields. This MUST be the first line.
        super(propertyId, address, rentAmount);

        // 'this.floorNumber' refers to the FIELD declared above.
        // 'floorNumber' (without 'this') refers to the PARAMETER passed into the constructor.
        // We assign the parameter value to the field so the object remembers it.
        this.floorNumber = floorNumber;

        // Same pattern: assign the unit number parameter to the field
        this.unitNumber = unitNumber;

        // Assign the floor size parameter to the field
        this.floorSize = floorSize;

        // Assign the elevator access parameter to the field
        this.elevatorAccess = elevatorAccess;
    }

    // GETTER METHODS (Accessors)
    // These PUBLIC methods allow OTHER classes to READ the private fields.
    // This is part of ENCAPSULATION — controlled access to internal data.

    // Returns the floor number of this flat
    public int getFloorNumber() {
        return floorNumber;   // Returns the value stored in the floorNumber field
    }

    // Returns the unit number (e.g., "A", "101")
    public String getUnitNumber() {
        return unitNumber;
    }

    // Returns the floor size in square meters
    public double getFloorSize() {
        return floorSize;
    }

    // Returns true if the flat has elevator access, false otherwise
    public boolean isElevatorAccess() {
        return elevatorAccess;
        // Note: boolean getters typically use 'is' instead of 'get' by Java convention
    }

    // SETTER METHODS (Mutators)
    // These PUBLIC methods allow OTHER classes to MODIFY the private fields.
    // Useful if Mrs James needs to update flat details after creation.

    // Updates the floor number
    public void setFloorNumber(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    // Updates the unit number
    public void setUnitNumber(String unitNumber) {
        this.unitNumber = unitNumber;
    }

    // Updates the floor size
    public void setFloorSize(double floorSize) {
        this.floorSize = floorSize;
    }

    // Updates elevator access status
    public void setElevatorAccess(boolean elevatorAccess) {
        this.elevatorAccess = elevatorAccess;
    }

    // OVERRIDDEN METHOD — POLYMORPHISM IN ACTION
    // The parent class (Property) declared this method as 'abstract',
    // meaning every subclass MUST provide its own implementation.
    // This version is customized specifically for Flat objects.
    @Override
    public String getPropertyDetails() {
        // Builds and returns a descriptive string about this flat.
        // getAddress() and getRentAmount() are INHERITED from Property.
        return "Flat at " + getAddress()
                + " | Unit: " + unitNumber
                + " | Rent: N$" + getRentAmount()
                + " | Floor: " + floorNumber
                + " | Size: " + floorSize + " m²"
                + " | Elevator: " + (elevatorAccess ? "Yes" : "No");
        // The ternary operator (condition ? trueValue : falseValue) converts
        // the boolean elevatorAccess into a readable "Yes" or "No" string.
    }
}