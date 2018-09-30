package com.akgs.dronedeliverysystem;

public class ItemNotPresentException extends Exception {
    ItemNotPresentException(){
        super("Item not present in warehouse");
    }
}
