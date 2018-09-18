package com.akgs.interview.scriptbox.dronedeliverysystem;

import java.io.Serializable;

public class DroneEvent implements Serializable {
    public Drone.STATE type;
    public String droneName;
    public String message;
    public DroneEvent(String name, Drone.STATE type, String message){
        this.droneName = name;
        this.type = type;
        this.message = message;
    }
}
