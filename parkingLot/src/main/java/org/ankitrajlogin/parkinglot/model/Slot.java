package org.ankitrajlogin.parkinglot.model;


import org.ankitrajlogin.parkinglot.enums.SlotType;

public class Slot {
    private final int slotNumber ;
    private final SlotType slotType ;
    private Vehicle parkedVehicle ;

    public Slot(int slotNumber , SlotType type){
        this.slotNumber = slotNumber ;
        this.slotType = type ;
    }

    public int getSlotNumber(){
        return slotNumber ;
    }

    public SlotType getSlotType() {
        return slotType;
    }

    public boolean isFree(){
        return parkedVehicle == null ;
    }

    public boolean park(Vehicle vehicle){
        if(!isFree()) return false ;
        this.parkedVehicle = vehicle ;
        return true ;
    }

    public Vehicle unpark(){
        Vehicle v = parkedVehicle ;
        parkedVehicle = null ;
        return v ;
    }

    public Vehicle getParkedVehicle(){
        return parkedVehicle ;
    }
}
