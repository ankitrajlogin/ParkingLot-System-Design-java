package org.ankitrajlogin.parkinglot.model;

import org.ankitrajlogin.parkinglot.enums.SlotType;
import org.ankitrajlogin.parkinglot.enums.VehicleType;

public class ParkingUtils {
    public static SlotType slotTypeForVehicle(VehicleType v) {
        switch (v) {
            case BIKE:
                return SlotType.SMALL;
            case TRUCK:
                return SlotType.LARGE;
            case CAR:
            default:
                return SlotType.MEDIUM;
        }
    }
}
