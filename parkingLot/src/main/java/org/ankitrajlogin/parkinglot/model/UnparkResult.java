package org.ankitrajlogin.parkinglot.model;

/**
 * Result wrapper for unpark operation.
 */
public class UnparkResult {
    public final boolean valid ;
    public final Vehicle vehicle ;

    public UnparkResult(boolean valid , Vehicle vehicle){
        this.valid = valid ;
        this.vehicle = vehicle ;
    }
}
