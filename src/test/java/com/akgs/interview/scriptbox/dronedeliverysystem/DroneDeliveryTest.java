package com.akgs.interview.scriptbox.dronedeliverysystem;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedList;

import static org.junit.Assert.assertEquals;

public class DroneDeliveryTest {
    private static CommandCenter cc;
    private static Warehouse warehouse;
    private static Drone drone;
    @BeforeClass
    public static void init(){
        cc = CommandCenter.getInstance();
        warehouse = cc.getWarehouse();
        //Create drone
        String droneName = "Up-Up-And-Away";
        drone = cc.createDrone(droneName);
    }

    @Test(expected = ItemNotPresentException.class)
    public void checkItemNotPresentExceptionThrown() throws ItemNotPresentException{


        //Set destination
        Coordinates destination = new Coordinates("Destination", 110, 110);
        cc.pickupAndDeliverItem(drone, "Item1", destination);
    }

    @Test(expected = ItemNotLoadedException.class)
    public void deliverItemWithoutPickUp() throws ItemNotLoadedException{
        //Create drone
        String droneName = "Up-Up-And-Away";
        Drone drone = cc.createDrone(droneName);

        //Set destination
        Coordinates destination = new Coordinates("Destination", 110, 110);
        drone.deliverItem(destination);
    }

    @Test(expected = ItemNotLoadedException.class)
    public void unloadItemWithoutLoading() throws ItemNotLoadedException{
        //Create drone
        String droneName = "Up-Up-And-Away";
        Drone drone = cc.createDrone(droneName);

        //Set destination
        Coordinates destination = new Coordinates("Destination", 110, 110);
        drone.unloadItem();
    }

    @Test
    public void deliverItemAndMoveToParkingSpot() throws ItemNotPresentException{
        warehouse.addItem("Item1");
        //Set destination
        Coordinates destination = new Coordinates("Destination", 110, 110);
        cc.pickupAndDeliverItem(drone, "Item1", destination);
        try {
            Thread.sleep(12000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LinkedList<DroneEvent> eventQueue = (LinkedList<DroneEvent>) cc.getEventCabin().get(drone.getName());
        //Last Event in the queue should be 'Waiting in Parking spot'
        assertEquals(Drone.STATE.IN_PARKING_SPOT, ((DroneEvent)eventQueue.getLast()).type);
    }
}
