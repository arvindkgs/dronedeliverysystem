Build command (Creates a jar-with-dependencies)
-----------------------------------------------
mvn clean dependency:copy-dependencies package

Run command (from dronedeliverysystem folder)
---------------------------------------------
java -jar target/drone-delivery-system-1.0-SNAPSHOT-jar-with-dependencies.jar

Drone Delivery System
---------------------
Provides you with following objects
* CommandCenter - Hides boilerplate code, you can get a WareHouse object, command drone to deliver to given destination. It also provides you with an EventCabin (its a collection(Map) of multiple queues, each queue that captures events for a separate drone)
* Warehouse - Add drones(to reserve parking spot), add items.
* Drone - Takes instructions like loadItem, unloadItem, moveToLocation, pickupItem(combination of moveToLocation and loadItem), deliverItem(combination of moveToLocation and unloadItem), returnToParkingSpot
* DroneEvent - This are Events sent to a JMS queue by the drones. 
* Coordinates - Coordinates to send the drone
* Utility - Helper methods
* DroneDeliveryTest - Junit Tests
* Constants

I have also added Logging via Log4J and reading from properties file, to allow easy configuration.

A typical flow is - 
1. CommandCenter is initiated.
2. CommandCenter initializes an EventQueue 'Drone.message.queue' either from existing JMS broker, if 'BROKER_URL' properties is configured in configuration.properties or from Constants property with value = tcp://localhost:61616
3. and creates a listener 'eventHandler' - reads Events from all drones and adds it to EventCabin object.
4. Items are added to the warehouse.
5. Drone is added to warehouse.
6. CommandCenter instructs the drone to deliver item to given location.

Further enhancements
1. CommandCenter can expose listener interface that can be called by the eventHandler when events are recieved by the eventHandler. So users can invoke custom logic to act on the different events thrown by the drones.