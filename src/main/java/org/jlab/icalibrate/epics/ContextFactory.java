package org.jlab.icalibrate.epics;

import com.cosylab.epics.caj.CAJContext;
import gov.aps.jca.CAException;
import gov.aps.jca.configuration.DefaultConfiguration;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is a service locator / factory for obtaining EPICS contexts.
 *
 * This class wraps around a ContextPool and provides life cycle management.
 *
 * <p>
 * A resource bundle named epics.properties is consulted to determine the EPICS addr_list value.</p>
 *
 * <p>
 * This class is a singleton which is created on application startup and destroyed on application
 * shutdown.</p>
 *
 * @author ryans
 */
final class ContextFactory {

    private static final Logger LOGGER = Logger.getLogger(ContextFactory.class.getName());
    private static ContextFactory INSTANCE = null;

    private ContextPool pool = null;

    private ContextFactory() {
        // Private constructor
        construct();
    }

    /**
     * Get the ContextFactory instance. A new ContextFactory is created if one does not already
     * exist.
     *
     * @return The one and only ContextFactory
     */
    public static ContextFactory getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ContextFactory();
        }

        return INSTANCE;
    }

    /**
     * Get an EPICS channel access context. Make sure to return it when you're done.
     *
     * @return the context.
     * @throws CAException if unable to get the context.
     */
    public CAJContext getContext() throws CAException {
        return pool.getContext();
    }

    /**
     * Return a context to the factory. This method should always be called when done with a
     * context.
     *
     * @param context the context.
     * @throws CAException if unable to return the context.
     */
    public void returnContext(CAJContext context) throws CAException {
        pool.returnContext(context);
    }

    /**
     * Construct the context factory.
     */
    private void construct() {
        LOGGER.log(Level.FINEST, "Constructing ContextPoolFactory");
        DefaultConfiguration config = new DefaultConfiguration("myconfig");

        String addrList;

        ResourceBundle bundle = ResourceBundle.getBundle("epics");

        if ("true".equals(bundle.getString("addr_from_env"))) {
            addrList = System.getenv("EPICS_CA_ADDR_LIST");
        } else {
            addrList = bundle.getString("addr_list");
        }

        LOGGER.log(Level.FINEST, "Using addr_list: {0}", addrList);

        config.setAttribute("addr_list", addrList);
        config.setAttribute("auto_addr_list", "false");

        try {
            pool = new ContextPool(config);
        } catch (CAException e) {
            LOGGER.log(Level.SEVERE, "Unable to create channel access context", e);
        }
    }

    /**
     * Destroy the context factory.
     */
    public void destruct() {
        LOGGER.log(Level.FINEST, "Destroying ContextPoolFactory");
        try {
            pool.destroy();
        } catch (CAException e) {
            LOGGER.log(Level.SEVERE, "Unable to destroy channel access context", e);
        }
    }
}
