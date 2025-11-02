package org.ankitrajlogin.parkinglot.model;

public class Ticket {
    private final String ticketId; // format: <parking_lot_id>_<floor_no>_<slot_no>
    private final String parkingLotId;
    private final int floorNo;
    private final int slotNo;
    private final Vehicle vehicle;

    public Ticket(String parkingLotId, int floorNo, int slotNo, Vehicle vehicle) {
        this.parkingLotId = parkingLotId;
        this.floorNo = floorNo;
        this.slotNo = slotNo;
        this.vehicle = vehicle;
        this.ticketId = parkingLotId + "_" + floorNo + "_" + slotNo + vehicle.getRegistrationNumber() ;
    }

    public String getTicketId() {
        return ticketId;
    }

    public int getFloorNo() {
        return floorNo;
    }

    public int getSlotNo() {
        return slotNo;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public String getRegistrationNumber(){
        return vehicle.getRegistrationNumber() ;
    }
}
