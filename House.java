/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


/**
 *
 * @author tjiro
 */
// House class extends Property and adds bedrooms attribute
public class House extends Property {
    private int bedrooms;

    // Constructor for House
    public House(int propertyId, String address, double rentAmount, int bedrooms) {
        super(propertyId, address, rentAmount); // call parent constructor
        this.bedrooms = bedrooms;
    }

    // Getter for bedrooms
    public int getBedrooms() {
        return bedrooms;
    }

    // Implementation of abstract method
    @Override
    public String getPropertyDetails() {
        return "House at " + getAddress() + " | Rent: " + getRentAmount() + " | Bedrooms: " + bedrooms;
    }
}

