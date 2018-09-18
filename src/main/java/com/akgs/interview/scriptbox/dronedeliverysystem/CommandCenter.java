package com.akgs.interview.scriptbox.dronedeliverysystem;

import ch.qos.logback.classic.Logger;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

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
    private static Logger logger = (Logger) LoggerFactory.getLogger(CommandCenter.class);

    /**
     * pickupAndDeliverItem
     * Creates a thread and executes the commands
     * pickup 'Item1',
     * deliver to destination,
     * return to parking spot.
     * @param drone
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
            } else {
                logger.error("Invalid Message Received in Queue");
            }
        }
    }


}
