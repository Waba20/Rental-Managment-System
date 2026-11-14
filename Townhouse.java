/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


/**
 *
 * @author tjiro
 */
// Townhouse class extends Property and adds garage attribute
public class Townhouse extends Property {
    private boolean hasGarage;

    // Constructor for Townhouse
    public Townhouse(int propertyId, String address, double rentAmount, boolean hasGarage) {
        super(propertyId, address, rentAmount);
        this.hasGarage = hasGarage;
    }

    // Getter for garage
    public boolean hasGarage() {
        return hasGarage;
    }

    // Implementation of abstract method
    @Override
    public String getPropertyDetails() {
        return "Townhouse at " + getAddress() + " | Rent: " + getRentAmount() + " | Garage: " + hasGarage;
    }
}
