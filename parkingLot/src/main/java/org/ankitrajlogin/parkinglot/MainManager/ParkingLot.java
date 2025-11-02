package org.ankitrajlogin.parkinglot.MainManager;


import org.ankitrajlogin.parkinglot.enums.SlotType;
import org.ankitrajlogin.parkinglot.enums.VehicleType;
import org.ankitrajlogin.parkinglot.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ParkingLot - main manager.
 */

public class ParkingLot {
    private final String parkingLotId ;
    private final int numberOfFloors ;
    private final int slotsPerFloors ;
    private final List<Floor> floors ;
    private final Map<String , Ticket> activeTicketsById ;

    public ParkingLot(String parkingLotId , int numberOfFloors , int slotsPerFloors){
        this.parkingLotId = parkingLotId ;
        this.numberOfFloors = numberOfFloors ;
        this.slotsPerFloors = slotsPerFloors ;
        this.floors = new ArrayList<>(numberOfFloors) ;

        for(int i = 1 ; i<= numberOfFloors ; i++){
            floors.add(new Floor(i , slotsPerFloors)) ;
        }

        this.activeTicketsById = new HashMap<>() ;
    }

    int getNumberOfFloor(){
        return numberOfFloors ; 
    }

    int getNumberOfSlotsInAFloor(){
        return slotsPerFloors ; 
    }

    /**
     * Park vehicle following rules:
     *  - Slot type must match vehicle
     *  - Lowest floor (1..n)
     *  - Lowest slot number (1..m)
     *
     * Returns ticket id if parked, null if no slot available.
     */

    public synchronized String parkVehicle(Vehicle vehicle){
        SlotType required = ParkingUtils.slotTypeForVehicle(vehicle.getType()) ;

        for(Floor f : floors){
            Slot slot = f.findFirstFreeSlotByType(required) ;

            if(slot != null){
                boolean parked = slot.park(vehicle) ;

                if(!parked) continue ;

                Ticket ticket = new Ticket(parkingLotId , f.getFloorNumber() , slot.getSlotNumber() , vehicle) ;
                activeTicketsById.put(ticket.getTicketId(), ticket) ;
                return ticket.getTicketId();

            }
        }
        return null ; // no slot found ;
    }

    /**
     * Unpark vehicle by ticket id.
     * Returns UnparkResult: valid=false => invalid ticket else unpack and free the slot ;
     */

    public synchronized UnparkResult unparkVehicle(String ticketId){
        Ticket t = activeTicketsById.get(ticketId) ;

        if(t == null){
            return new UnparkResult(false , null) ;
        }

        int floorNo = t.getFloorNo() ;
        int slotNo = t.getSlotNo() ;

        // Validate floor and slot numbers
        if(floorNo < 1 || floorNo > floors.size()){
            return new UnparkResult(false , null) ;
        }

        Floor floor = floors.get(floorNo -1 ) ;

        if(slotNo < 1 || slotNo > floor.getSlots().size()){
            return new UnparkResult(false , null) ;
        }

        Slot slot = floor.getSlots().get(slotNo -1 ) ;
        if(slot.isFree()){
            // slot not occupied -> invalid
            return new UnparkResult(false , null) ;
        }

        Vehicle v = slot.unpark() ;
        activeTicketsById.remove(ticketId) ;
        return new UnparkResult(true , v) ;

    }

    /**
     * Display number of free slots for vehicle type on each floor
     * Format:
     * No. of free slots for <VEHICLE> on Floor <floor_no>: <count>
     */
    public void displayFreeCount(VehicleType vtype) {
        for (Floor f : floors) {
            int count = f.getFreeSlotNumbersForVehicleType(vtype).size();
            System.out.println("No. of free slots for " + vtype.name() + " on Floor " + f.getFloorNumber() + ": " + count);
        }
    }

    /**
     * Display free slot numbers for vehicle type on each floor
     * Format:
     * Free slots for <VEHICLE> on Floor <floor_no>: <comma_separated_values_of_slot_nos>
     * If none, leave empty after colon (single space then nothing)
     */
    public void displayFreeSlots(VehicleType vtype) {
        for (Floor f : floors) {
            List<Integer> free = f.getFreeSlotNumbersForVehicleType(vtype);
            System.out.print("Free slots for " + vtype.name() + " on Floor " + f.getFloorNumber() + ": ");
            if (free.isEmpty()) {
                System.out.println();
            } else {
                System.out.println(joinInts(free));
            }
        }
    }

    /**
     * Display free slot numbers for vehicle type on each floor
     * Format:
     * Free slots for <VEHICLE> on Floor <floor_no>: <comma_separated_values_of_slot_nos>
     * If none, leave empty after colon (single space then nothing)
     */
    public void displayOccupiedSlots(VehicleType vtype) {
        for (Floor f : floors) {
            List<Integer> occ = f.getOccupiedSlotNumbersForVehicleType(vtype);
            System.out.print("Occupied slots for " + vtype.name() + " on Floor " + f.getFloorNumber() + ": ");
            if (occ.isEmpty()) {
                System.out.println();
            } else {
                System.out.println(joinInts(occ));
            }
        }
    }

    private String joinInts(List<Integer> list) {
        return list.stream().map(String::valueOf).collect(Collectors.joining(","));
    }




}
