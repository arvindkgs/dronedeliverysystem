package com.akgs.interview.scriptbox.dronedeliverysystem;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.activemq.broker.BrokerService;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Utility {
    public static Properties prop;
    private static BrokerService broker;
    private static Logger logger = (Logger) LoggerFactory.getLogger(Utility.class);

    static {
        try {
            prop = new Properties();
            String propFileName = "configuration.properties";
            InputStream inputStream = new Object() { }.getClass().getClassLoader().getResourceAsStream(propFileName);
            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }
        } catch (IOException ex) {
            logger.error("Error reading configuration.properties", ex);
        }
    }

    /*
     * Creates default Message Broker on localhost
     */
    private static BrokerService createBroker() throws Exception {
        if (broker == null) {
            Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.ERROR);
            broker = new BrokerService();
            broker.addConnector(Constants.BROKER_URL);
            broker.start();
        }
        return broker;
    }

    public static String getBrokerURL() {
        //Check if Message Broker is present
        if (prop.getProperty("BROKER_URL") != null) {
            return prop.getProperty("BROKER_URL");
        }
        //Initialize Default Message Broker Queue
        try {
            Utility.createBroker();
        } catch (Exception e) {
            logger.error("Unable to create Message Broker on " + Constants.BROKER_URL, e);
            System.exit(1);
        }
        return Constants.BROKER_URL;
    }

    public static int getThreadPoolSize() {
        return prop.getProperty("THREAD_POOL_SIZE") != null ?
                Integer.parseInt(prop.getProperty("THREAD_POOL_SIZE")) :
                Constants.THREAD_POOL_SIZE;
    }

    public static void stopBroker() {
        if (broker != null) {
            try {
                broker.stop();
            } catch (Exception e) {
                logger.error("Unable to stop broker", e);
            }
        }
    }
}
