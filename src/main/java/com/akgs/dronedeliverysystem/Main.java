package com.akgs.dronedeliverysystem;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Queue;
import java.util.Scanner;

/**
 * @author Arvind Kumar GS
 */
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
        String droneName = "1.Up-Up-And-Away";
        cc.createDrone(droneName);
        droneName = "2.Flyby";
        cc.createDrone(droneName);
        System.out.println("Creating drone('Flyby')");
        System.out.println("Setting destination.");
        //Set destination
        Coordinates destination = new Coordinates("Destination", 110, 110);

        //Add multiple delivery tasks
        cc.addTask("Item1", CommandCenter.TASK_PRIORITY.PRIME, destination);
        cc.addTask("Item2", CommandCenter.TASK_PRIORITY.STANDARD, destination);
        cc.addTask("Item3", CommandCenter.TASK_PRIORITY.PRIME, destination);

        //Instruct the CommandCenter to send the drone 'Up-Up-And-Away' following commands,
        // * pickup 'Item1',
        // * deliver to destination,
        // * return to parking spot.
        //these instructions will be carried out asynchronously by the drone.
        System.out.println("Commanding drone to pickup item 'Item1' and deliver to 'Destination'");
        try {
            cc.scheduleTaskDeliveries();
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
        HashMap<String, Queue<DroneEvent>> eventCabin = cc.getEventCabin();
        for (String drone : eventCabin.keySet()) {
            Iterator<DroneEvent> iterator = eventCabin.get(drone).iterator();
            System.out.println("-------------Start Drone:" + drone + "------------------------");
            while (iterator.hasNext()) {
                DroneEvent message = (DroneEvent) iterator.next();
                System.out.println("Event: " + message.type + " : " + message.message);
            }
            System.out.println("-------------End Drone:" + drone + "------------------------");
        }
    }
}
