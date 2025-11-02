package org.ankitrajlogin.parkinglot;


import org.ankitrajlogin.parkinglot.MainManager.ParkingLot;
import org.ankitrajlogin.parkinglot.enums.VehicleType;
import org.ankitrajlogin.parkinglot.model.UnparkResult;
import org.ankitrajlogin.parkinglot.model.Vehicle;

// import java.io.FileInputStream;
// import java.io.FileNotFoundException;
// import java.io.FileOutputStream;
// import java.io.PrintStream;
import java.util.Scanner;

/**
 * Main class - CLI entrypoint for Parking Lot system.
 * Implements commands:
 *  - create_parking_lot <parking_lot_id> <no_of_floors> <no_of_slots_per_floor>
 *  - park_vehicle <vehicle_type> <reg_no> <color>
 *  - unpark_vehicle <ticket_id>
 *  - display <display_type> <vehicle_type>  (display_type: free_count, free_slots, occupied_slots)
 *  - exit
 *
 * This program follows the specification provided in the problem statement.
 */



public class Main {
    public static void main(String[] args) {

        //  Uncomment Belows Line : if want to take input from the file and print result in output.txt file ;
//        String inputFile = "input.txt";
//        String outputFile = "output.txt";
//
//        try{
//            // Redirect System.in and System.out to files
//            System.setIn(new FileInputStream(inputFile));
//            System.setOut(new PrintStream(new FileOutputStream(outputFile))) ;
//        }
//        catch (FileNotFoundException e) {
//            System.err.println("File not found: " + e.getMessage());
//        }



        Scanner sc = new Scanner(System.in);
        ParkingLot parkingLot = null;

        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;
            if (line.equalsIgnoreCase("exit")) {
                break;
            }

            String[] parts = line.split("\\s+");
            String cmd = parts[0];

            switch (cmd) {
                case "create_parking_lot":
                    if (parts.length != 4) {
                        // ignoring malformed input as per spec (testcases expected correct format)
                        break;
                    }

                    String id = parts[1];
                    int floors = Integer.parseInt(parts[2]);
                    int slotsPerFloor = Integer.parseInt(parts[3]);
                    parkingLot = new ParkingLot(id, floors, slotsPerFloor);
                    System.out.println("Created parking lot with " + floors + " floors and " + slotsPerFloor + " slots per floor");
                    break;

                case "park_vehicle":
                    if (parkingLot == null) {
                        // not initialized; spec expects create first
                        break;
                    }
                    if (parts.length < 4) break;
                    String vtypeStr = parts[1];
                    String reg = parts[2];
                    // color may contain no spaces per input spec; safe to take parts[3]
                    String color = parts[3];
                    VehicleType vtype;

                    try {
                        vtype = VehicleType.valueOf(vtypeStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // unknown vehicle type
                        break;
                    }
                    Vehicle v = new Vehicle(vtype, reg, color);
                    String ticketId = parkingLot.parkVehicle(v);

                    if (ticketId == null) {
                        System.out.println("Parking Lot Full");
                    } else {
                        System.out.println("Parked vehicle. Ticket ID: " + ticketId);
                    }
                    break;

                case "unpark_vehicle":
                    if (parkingLot == null) break;
                    if (parts.length != 2) break;
                    String ticket = parts[1];

                    UnparkResult res = parkingLot.unparkVehicle(ticket);
                    if (!res.valid) {
                        System.out.println("Invalid Ticket");
                    } else {
                        System.out.println("Unparked vehicle with Registration Number: " + res.vehicle.getRegistrationNumber()
                                + " and Color: " + res.vehicle.getColor());
                    }
                    break;

                case "display":
                    if (parkingLot == null) break;
                    if (parts.length != 3) break;
                    String displayType = parts[1];
                    String dispVehicleTypeStr = parts[2];
                    VehicleType dispVType;
                    try {
                        dispVType = VehicleType.valueOf(dispVehicleTypeStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        break;
                    }

                    switch (displayType) {
                        case "free_count":
                            parkingLot.displayFreeCount(dispVType);
                            break;
                        case "free_slots":
                            parkingLot.displayFreeSlots(dispVType);
                            break;
                        case "occupied_slots":
                            parkingLot.displayOccupiedSlots(dispVType);
                            break;
                        default:
                            // invalid display command
                            break;
                    }
                    break;

                default:
                    // unknown command - ignore or handle as needed
                    break;

            }
        }

        sc.close() ; 
    }
}