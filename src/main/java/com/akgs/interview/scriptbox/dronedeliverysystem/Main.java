package com.akgs.interview.scriptbox.dronedeliverysystem;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(new InputStreamReader(System.in));
        System.out.println("---------------------------------------------------------------------");
        System.out.println("                        Drone Delivery System                        ");
        System.out.println("---------------------------------------------------------------------");

        System.out.println("Initializing Command Center....");
        //Initialize
        CommandCenter cc = CommandCenter.getInstance();
        Warehouse warehouse = cc.getWarehouse();

        System.out.println("Adding 3 items('Item1','Item2','Item2') to the warehouse");
        //Add Items to warehouse
        warehouse.addItem("Item1");
        warehouse.addItem("Item2");
        warehouse.addItem("Item3");

        System.out.println("Creating drone('Up-Up-And-Away')");
        //Create drone
        String droneName = "Up-Up-And-Away";
        Drone drone = cc.createDrone(droneName);
        System.out.println("Setting destination.");
        //Set destination
        Coordinates destination = new Coordinates("Destination", 110, 110);

        //Instruct the CommandCenter to send the drone 'Up-Up-And-Away' following commands,
        // * pickup 'Item1',
        // * deliver to destination,
        // * return to parking spot.
        //these instructions will be carried out asynchronously by the drone.
        System.out.println("Commanding drone to pickup item 'Item1' and deliver to 'Destination'");
        try {
            cc.pickupAndDeliverItem(drone, "Item1", destination);
        } catch (ItemNotPresentException e) {
            System.out.println(e.getMessage());
        }

        //Stop consuming drone events when user press 'ctrl-d'
        System.out.println("To stop program and read event queue, Press 'ctrl-d'.");
        System.out.println("If the last event is not 'Waiting in Parking Spot', you need wait longer before, reading event queue.");
        try {
            while ((System.in.read()) != -1) ;
        } catch (IOException e) {
        }
        cc.stop();

        //Read Messages
        System.out.println("--------------------------Reading Event Queue---------------------------");
        Iterator<DroneEvent> iterator = cc.iterateEventLog(droneName);
        while (iterator.hasNext()) {
            DroneEvent message = (DroneEvent) iterator.next();
            System.out.println("Drone: " + message.droneName + ", Event: " + message.type + " : " + message.message);
        }
    }
}
