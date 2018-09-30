package com.akgs.dronedeliverysystem;

import ch.qos.logback.classic.Logger;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.*;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * @author Arvind Kumar GS
 */

/*
 * This is a Singleton Class
 * It initializes an EventQueue 'Drone.message.queue' either
 *  - from existing JMS broker, if 'BROKER_URL' properties is configured in configuration.properties
 *  - else from tcp://localhost:61616
 * and creates a listener
 * 'eventHandler' - reads Events from all drones and adds it to,
 * EventCabin, its a collection(Map) of multiple queues, each queue that captures events for a separate drone
 */
public class CommandCenter {
    private static CommandCenter instance;
    private String brokerURL;
    private Destination destination;
    private Session session;
    private Warehouse warehouse;
    private ExecutorService executor;
    private DroneEventHandler eventHandler;
    private Connection connection;
    private Thread jmsQueueReader;
    private Map<String, Drone> drones;
    private PriorityQueue<Task> tasks;
    private static Logger logger = (Logger) LoggerFactory.getLogger(CommandCenter.class);

    /**
     * pickupAndDeliverItem
     * Creates a thread and executes the commands
     * pickup 'Item1',
     * deliver to destination,
     * return to parking spot.
     * @param {Drone} drone - drone that picksup the item
     * @param item
     * @param destination
     * @throws ItemNotPresentException
     */
    public void pickupAndDeliverItem(final Drone drone, final String item, final Coordinates destination) throws ItemNotPresentException {
        final Coordinates location = warehouse.getItemLocation(item);
        if (location == null)
            throw new ItemNotPresentException();

        FutureTask<Boolean> pickupAndDeliverItemTask = new FutureTask<Boolean>(new Callable() {
            public Boolean call() throws ItemNotLoadedException {
                return drone.pickupItem(location, item) &&
                        drone.deliverItem(destination) &&
                        drone.returnToParkingSpot(warehouse.getParkingSpot(drone));
            }
        });

        executor.execute(pickupAndDeliverItemTask);
    }

    public Drone createDrone(String name) {
        Drone d = new Drone(name, session, destination);
        drones.put(name, d);
        warehouse.addDrone(d);
        return d;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    private CommandCenter() {
        brokerURL = Utility.getBrokerURL();
        //Create session
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(brokerURL);
        try {
            connection = cf.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            destination = session.createQueue(Constants.DRONE_QUEUE);
            eventHandler = new DroneEventHandler();
            connection.start();
            jmsQueueReader = new Thread(new Runnable() {
                public void run() {
                    try {
                        MessageConsumer consumer = session.createConsumer(destination);
                        consumer.setMessageListener(eventHandler);
                        while (!Thread.currentThread().isInterrupted()) ;
                    } catch (JMSException e) {
                    }
                }
            });
            jmsQueueReader.start();
        } catch (JMSException e) {
            logger.error("Not able to configure event reader process. Check if broker is started at: "+Utility.getBrokerURL());
            System.exit(1);
        }
        tasks = new PriorityQueue<Task>();
        drones = new HashMap<String, Drone>();
        warehouse = new Warehouse("BangaloreWarehouse", new Coordinates(100, 100));
        executor = Executors.newFixedThreadPool(Utility.getThreadPoolSize());
    }

    /**
     * Stops broker, Executor service, And EventReader
     */
    public void stop() {
        try {
            connection.close();
            Utility.stopBroker();
            executor.shutdown();
            jmsQueueReader.interrupt();
        } catch (JMSException e1) {
            logger.error("Unable to stop command center due to message queue or broker or thread pool", e1);
        }
    }


    public static CommandCenter getInstance() {
        if (instance == null) {
            instance = new CommandCenter();
        }
        return instance;
    }

    public Iterator<DroneEvent> iterateEventLog(String droneName) {
        return eventHandler.eventCabin.get(droneName).iterator();
    }

    public HashMap<String, Queue<DroneEvent>> getEventCabin(){
        return eventHandler.eventCabin;
    }

    public void addTask(String item1, TASK_PRIORITY priority, Coordinates destination) {
        tasks.add(new Task(item1, priority, destination));
    }

    public void scheduleTaskDeliveries() throws ItemNotPresentException {
        //Schedule a delivery while tasks are still present
        for (String droneName : drones.keySet()) {
            if (tasks.isEmpty()) {
                break;
            }
            Drone drone = drones.get(droneName);
            Task t = tasks.poll();
            pickupAndDeliverItem(drone, t.item, t.destination);
        }
    }

    class DroneEventHandler implements MessageListener {
        HashMap<String, Queue<DroneEvent>> eventCabin;

        public DroneEventHandler() {
            eventCabin = new HashMap<String, Queue<DroneEvent>>();
        }

        public void onMessage(Message message) {
            if (message instanceof ObjectMessage) {
                DroneEvent droneEvent = null;
                try {
                    droneEvent = (DroneEvent) ((ObjectMessage) message).getObject();
                } catch (JMSException e) {
                    logger.error("Error while casting object to droneEvent", e);
                }
                if (eventCabin.get(droneEvent.droneName) == null) {
                    eventCabin.put(droneEvent.droneName, new LinkedList<DroneEvent>());
                }
                eventCabin.get(droneEvent.droneName).add(droneEvent);
                //System.out.println("Drone: " + droneEvent.droneName + ", Event: " + droneEvent.type + " : " + droneEvent.message);
                if (droneEvent.type == Drone.STATE.IN_PARKING_SPOT) {
                    //If drone is waiting in parking spot, and tasks still exists,
                    //assign the drone to that task
                    if (tasks.isEmpty())
                        return;
                    Task t = tasks.poll();
                    Drone d = drones.get(droneEvent.droneName);
                    try {
                        pickupAndDeliverItem(d, t.item, t.destination);
                    } catch (ItemNotPresentException e) {
                        logger.error("Item not present in warehouse: " + t.item);
                    }
                }
            } else {
                logger.error("Invalid Message Received in Queue");
            }
        }
    }

    class Task implements Comparable {
        String item;
        Coordinates destination;
        TASK_PRIORITY priority;

        private Task(String item1, TASK_PRIORITY priority, Coordinates destination) {
            item = item1;
            this.priority = priority;
            this.destination = destination;
        }

        public int compareTo(Object o) {
            if (o instanceof Task && ((Task) o).priority == TASK_PRIORITY.PRIME) {
                return 1;
            } else
                return -1;
        }
    }

    enum TASK_PRIORITY {
        PRIME, STANDARD;
    }
}
