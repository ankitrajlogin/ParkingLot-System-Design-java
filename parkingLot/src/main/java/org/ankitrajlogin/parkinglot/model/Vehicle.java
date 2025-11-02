package org.ankitrajlogin.parkinglot.model;

import org.ankitrajlogin.parkinglot.enums.VehicleType;

public class Vehicle {
    private final VehicleType type;
    private final String registrationNumber;
    private final String color;

    public Vehicle(VehicleType type, String registrationNumber, String color) {
        this.type = type;
        this.registrationNumber = registrationNumber;
        this.color = color;
    }

    public VehicleType getType() {
        return type;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getColor() {
        return color;
    }
}
