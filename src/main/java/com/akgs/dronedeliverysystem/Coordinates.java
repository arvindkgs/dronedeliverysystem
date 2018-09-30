package com.akgs.dronedeliverysystem;

public class Coordinates {
    private String name;
    private int x;
    private int y;
    public Coordinates(int x, int y){
        this.x = x;
        this.y = y;
    }
    public Coordinates(String name,int x, int y){
        this.x = x;
        this.y = y;
        this.name = name;
    }
    public String getName() { return name; }
    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }

}
