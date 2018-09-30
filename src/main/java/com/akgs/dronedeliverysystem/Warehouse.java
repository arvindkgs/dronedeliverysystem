package com.akgs.dronedeliverysystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Warehouse {
    private Map<Object, Coordinates> inventory;
    private Coordinates location;
    private ParkingRack parkingSpots;
    private String name;

    public Warehouse(String name, Coordinates location) {
        this.name = name;
        this.location = location;
        inventory = new HashMap<Object, Coordinates>();
        parkingSpots = new ParkingRack(location);
    }

    public String getName() {
        return name;
    }

    public Coordinates addDrone(Drone d) {
        return parkingSpots.addToRack(d);
    }

    public Coordinates getParkingSpot(Drone d) {
        return parkingSpots.getParkingSpot(d);
    }

    public boolean itemExists(Object item) {
        return inventory.containsKey(item);
    }

    public void addItem(Object item) {
        Coordinates itemLocation = new Coordinates("WareHouseItemLocation", location.getX() + 10 * inventory.size(), location.getY());
        inventory.put(item, itemLocation);
    }

    public Coordinates getItemLocation(Object item) {
        return itemExists(item) ? inventory.get(item) : null;
    }

    class ParkingRack {
        private List<Drone> rack;
        private Coordinates startLocation;

        public ParkingRack(Coordinates startLocation) {
            rack = new ArrayList<Drone>();
            this.startLocation = startLocation;
        }

        public Coordinates addToRack(Drone drone) {
            rack.add(drone);
            return new Coordinates(startLocation.getX(), startLocation.getY() + 10 * rack.size());
        }

        public Coordinates getParkingSpot(Drone drone) {
            return new Coordinates("Parking_Dock_" + drone.getName(), startLocation.getX(), startLocation.getY() + 10 * rack.indexOf(drone));
        }
    }
}
