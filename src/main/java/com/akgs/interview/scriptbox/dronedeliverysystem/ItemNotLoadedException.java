package com.akgs.interview.scriptbox.dronedeliverysystem;

public class ItemNotLoadedException extends Exception {
    ItemNotLoadedException(){
        super("Item not loaded into drone. First load item then call deliverItem.");
    }
}
