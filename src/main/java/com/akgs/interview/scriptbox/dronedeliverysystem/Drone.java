package com.akgs.interview.scriptbox.dronedeliverysystem;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

public class Drone {
    private static Logger logger = (Logger) LoggerFactory.getLogger(Drone.class);
    private Object item;
    private Coordinates currLocation;
    private STATE currState;
    private MessageProducer eventHandler;
    private Session session;
    private String name;

    public enum STATE {
        DELIVERING {
            public String toString() {
                return "Delivering item to destination";
            }
        }, RETURNING {
            public String toString() {
                return "Returning to parking spot after delivery";
            }
        }, LOADING_ITEM {
            public String toString() {
                return "Loading Item to drone";
            }
        }, ITEM_LOADED {
            public String toString() {
                return "Item loaded in drone";
            }
        }, UNLOADING_ITEM {
            public String toString() {
                return "Unloading item from drone";
            }
        }, ITEM_UNLOADED {
            public String toString() {
                return "Item unloaded at destination";
            }
        }, IN_PARKING_SPOT {
            public String toString() {
                return "Waiting in parking spot";
            }
        }, REACHED_DESTINATION {
            public String toString() {
                return "Reached destination";
            }
        }, MOVING {
            public String toString() {
                return "Moving to location";
            }
        }, MOVED_TO_LOCATION {
            public String toString() {
                return "Moved to location";
            }
        };
    }

    public String getName() {
        return name;
    }

    public Drone(String name, Session session, Destination destination) {
        try {
            this.name = name;
            this.session = session;
            eventHandler = session.createProducer(destination);
        } catch (JMSException e) {
            logger.error("Not able to configure event created process", e);
        }
    }


    public STATE getStatus() {
        return currState;
    }

    public boolean deliverItem(Coordinates destination) throws ItemNotLoadedException {
        if (currState != STATE.ITEM_LOADED) {
            throw new ItemNotLoadedException();
        }
        return (moveToLocation(destination) == destination && unloadItem());
    }

    public boolean pickupItem(Coordinates location, Object item) {
        return moveToLocation(location) == location && loadItem(item);
    }

    public Object getItem() {
        return item;
    }

    public boolean returnToParkingSpot(final Coordinates location) {
        return moveToLocation(location) == location && park();
    }

    private boolean park() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            logger.error("Thread interrupted during parking park");
            return false;
        }
        sendEvent(STATE.IN_PARKING_SPOT);
        return true;
    }

    private Coordinates moveToLocation(final Coordinates location) {
        sendEvent(STATE.MOVING, location.getName());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            logger.error("Thread interrupted during moving to location:" + location.getName());
            return null;
        }
        currLocation = location;
        sendEvent(STATE.MOVED_TO_LOCATION, location.getName());
        return location;
    }

    public boolean unloadItem() throws ItemNotLoadedException {
        if (item == null) {
            throw new ItemNotLoadedException();
        }
        sendEvent(STATE.UNLOADING_ITEM, (String) item);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.error("Thread interrupted during unloading item");
            return false;
        }
        this.item = null;
        sendEvent(STATE.ITEM_UNLOADED, (String) item);
        return true;
    }

    private boolean loadItem(Object item) {
        sendEvent(STATE.LOADING_ITEM, (String) item);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.error("Thread interrupted during loading item");
            return false;
        }
        this.item = item;
        sendEvent(STATE.ITEM_LOADED, (String) item);
        return true;
    }

    private void sendEvent(STATE state) {
        sendEvent(state, "");
    }

    private void sendEvent(STATE state, String message) {
        try {
            currState = state;
            eventHandler.send(session.createObjectMessage(new DroneEvent(name, state, message)));
        } catch (JMSException e) {
            logger.error("Error writing event to queue. The Session is closed");
        }
    }

}
