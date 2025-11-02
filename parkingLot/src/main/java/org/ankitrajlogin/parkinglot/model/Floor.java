package org.ankitrajlogin.parkinglot.model;

import org.ankitrajlogin.parkinglot.enums.SlotType;
import org.ankitrajlogin.parkinglot.enums.VehicleType;

import java.util.ArrayList;
import java.util.List;

public class Floor {
    private final int floorNumber ;
    private final List<Slot> slots ;

    public Floor(int floorNumber , int slotsPerFloor) {
        this.floorNumber = floorNumber;
        this.slots = new ArrayList<>(slotsPerFloor);
        /**
        per spec: first slot => truck (LARGE), next 2 => bikes (SMALL), remaining => cars (MEDIUM)
         // we can change as per requirement. like we can take the number of slots per floor for different cars .
         // public Floor(int floorNumber , int carSlot , int BikeSlot , int TruckSlot)
        */
        for(int i = 1; i<= slotsPerFloor ; i++){
            SlotType type ;
            if (i == 1) {
                type = SlotType.LARGE;
            } else if (i == 2 || i == 3) {
                type = SlotType.SMALL;
            } else {
                type = SlotType.MEDIUM;
            }
            slots.add(new Slot(i, type));
        }
    }

    public int getFloorNumber(){
        return floorNumber ;
    }

    public List<Slot> getSlots(){
        return slots ;
    }

    /**
     * Find first free slot of given SlotType on this floor (lowest slot number).
     */
    public Slot findFirstFreeSlotByType(SlotType required) {
        for (Slot s : slots) {
            if (s.getSlotType() == required && s.isFree()) {
                return s;
            }
        }
        return null;
    }

    /**
     * Returns list of occupied slot numbers (sorted ascending) for a vehicle type.
     */
    public List<Integer> getOccupiedSlotNumbersForVehicleType(VehicleType vtype) {
        SlotType required = ParkingUtils.slotTypeForVehicle(vtype);
        List<Integer> res = new ArrayList<>();
        for (Slot s : slots) {
            if (s.getSlotType() == required && !s.isFree()) {
                res.add(s.getSlotNumber());
            }
        }
        return res;
    }

    /**
     * Returns list of free slot numbers (sorted ascending) for a vehicle type.
     */
    public List<Integer> getFreeSlotNumbersForVehicleType(VehicleType vtype) {
        SlotType required = ParkingUtils.slotTypeForVehicle(vtype);
        List<Integer> res = new ArrayList<>();
        for (Slot s : slots) {
            if (s.getSlotType() == required && s.isFree()) {
                res.add(s.getSlotNumber());
            }
        }
        return res;
    }
}
