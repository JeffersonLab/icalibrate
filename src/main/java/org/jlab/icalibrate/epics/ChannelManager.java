package org.jlab.icalibrate.epics;

import com.cosylab.epics.caj.CAJChannel;
import com.cosylab.epics.caj.CAJContext;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.dbr.DBR;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Responsible for managing EPICS Channel Access.
 *
 * @author ryans
 */
public class ChannelManager implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(ChannelManager.class.getName());

    /**
     * Number of seconds to wait for IO operations before a timeout exception occurs.
     */
    public static final double PEND_TIMEOUT_SECONDS = 2.0d;

    private CAJContext context;
    private ScheduledExecutorService executor;    
    private final Map<String, ChannelMonitor> monitorMap = new HashMap<>();
    private final Map<PvListener, Set<String>> listenerMap = new HashMap<>();
    
    private final ContextFactory factory = ContextFactory.getInstance();

    /**
     * Create a new ChannelManager.
     */
    public ChannelManager() {
        construct();
    }

    /**
     * Initializes EPICS CA
     */
    private void construct() {
        LOGGER.log(Level.FINEST, "Creating ChannelMonitorManager");
        try {
            context = factory.getContext();
        } catch (CAException e) {
            LOGGER.log(Level.SEVERE, "Unable to obtain channel access context", e);
        }
        
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Cleans up EPICS CA
     */
    private void destruct() {
        LOGGER.log(Level.FINEST, "Destroying ChannelMonitorManager");
        try {
            factory.returnContext(context);
            factory.destruct();
        } catch (CAException e) {
            LOGGER.log(Level.WARNING, "Unable to return channel access context", e);
        }
        
        executor.shutdown();
    }

    /**
     * Perform a synchronous (blocking) CA-GET request of the given PV.
     *
     * @param pv The EPICS CA PV name
     * @return The EPICS DataBaseRecord
     * @throws CAException If unable to perform the CA-GET due to IO
     * @throws TimeoutException If unable to perform the CA-GET in a timely fashion
     */
    public DBR get(String pv) throws CAException, TimeoutException {

        CAJChannel channel = null;
        DBR dbr = null;

        try {
            channel = (CAJChannel) context.createChannel(pv);

            context.pendIO(PEND_TIMEOUT_SECONDS);

            dbr = channel.get();

            context.pendIO(PEND_TIMEOUT_SECONDS);
        } finally {
            if (channel != null) {
                channel.destroy();
            }
        }

        return dbr;
    }

    /**
     * Perform a synchronous (blocking) CA-PUT request of the given PV and value.
     *
     * @param pv The EPICS CA PV name
     * @param value The String value
     * @throws CAException If unable to perform the CA-GET due to IO
     * @throws TimeoutException If unable to perform the CA-GET in a timely fashion
     */
    public void put(String pv, String value) throws CAException, TimeoutException {

        CAJChannel channel = null;

        try {
            channel = (CAJChannel) context.createChannel(pv);

            context.pendIO(PEND_TIMEOUT_SECONDS);

            channel.put(value);

            context.pendIO(PEND_TIMEOUT_SECONDS);
        } finally {
            if (channel != null) {
                channel.destroy();
            }
        }

    }

    /**
     * Perform a synchronous (blocking) CA-PUT request of the given PV and value.
     *
     * @param pv The EPICS CA PV name
     * @param value The Number value
     * @throws CAException If unable to perform the CA-GET due to IO
     * @throws TimeoutException If unable to perform the CA-GET in a timely fashion
     */
    public void put(String pv, Number value) throws CAException, TimeoutException {

        CAJChannel channel = null;

        try {
            channel = (CAJChannel) context.createChannel(pv);

            context.pendIO(PEND_TIMEOUT_SECONDS);

            channel.put((value).doubleValue());

            context.pendIO(PEND_TIMEOUT_SECONDS);
        } finally {
            if (channel != null) {
                channel.destroy();
            }
        }

    }

    /**
     * Registers a PV monitor on the supplied PV for the given listener. Equivalent to calling
     * addPvs with a set of one PV.
     *
     * @param listener The PvListener
     * @param pv The EPICS PV name
     */
    public void addPv(PvListener listener, String pv) {
        HashSet<String> pvSet = new HashSet<>();
        pvSet.add(pv);
        addPvs(listener, pvSet);
    }

    /**
     * Registers PV monitors on the supplied PVs for the given listener. Note that internally only a
     * single monitor is used for any given PV. PVs for which the given listener is already
     * listening to are skipped (duplicate PVs are ignored). There is no need to call addListener
     * before calling this method.
     *
     * @param listener The PvListener to receive notifications
     * @param addPvSet The set of PVs to monitor
     */
    public void addPvs(PvListener listener, Set<String> addPvSet) {

        Set<String> newPvList = new HashSet<>();

        if (addPvSet != null) {
            newPvList.addAll(addPvSet);

            for (String pv : addPvSet) {
                //LOGGER.log(Level.FINEST, "addListener pv: {0}; pv: {1}", new Object[]{listener, pv});
                ChannelMonitor monitor = monitorMap.get(pv);

                if (monitor == null) {
                    //LOGGER.log(Level.FINEST, "Opening ChannelMonitor: {0}", pv);
                    monitor = new ChannelMonitor(pv, context, executor);
                    monitorMap.put(pv, monitor);
                } else {
                    //LOGGER.log(Level.FINEST, "Joining ChannelMonitor: {0}", pv);
                }

                monitor.addListener(listener);
            }
        }

        Set<String> oldPvList = listenerMap.get(listener);

        if (oldPvList != null) {
            newPvList.addAll(oldPvList);
        }

        listenerMap.put(listener, newPvList);
    }

    /**
     * Removes the supplied PVs from the given listener.
     *
     * @param listener The PvListener
     * @param clearPvSet The PV set to clear
     */
    public void clearPvs(PvListener listener, Set<String> clearPvSet) {

        Set<String> newPvList;
        Set<String> oldPvList = listenerMap.get(listener);

        if (oldPvList != null) {
            newPvList = new HashSet<>(oldPvList);
            newPvList.removeAll(clearPvSet);
        } else {
            newPvList = new HashSet<>();
        }

        removeFromChannels(listener, clearPvSet);
        listenerMap.put(listener, newPvList);
    }

    /**
     * Removes a listener from channels and if no listeners remain on a given channel then closes
     * the channel.
     *
     * @param listener The PvListener
     * @param pvList The PV list (and indirectly the channel list)
     */
    private void removeFromChannels(PvListener listener, Set<String> pvList) {
        if (pvList != null) { // Some clients don't immediately connect to a pv so have an empty pv list
            for (String pv : pvList) {
                ChannelMonitor monitor = monitorMap.get(pv);

                if (monitor != null) {
                    monitor.removeListener(listener);

                    if (monitor.getListenerCount() == 0) {
                        //LOGGER.log(Level.FINEST, "Closing ChannelMonitor: {0}", pv);
                        try {
                            monitor.close();
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "Unable to close monitor", e);
                        }
                        monitorMap.remove(pv);
                    }
                }
            }
        }
    }

    /**
     * A convenience method to add a listener without registering any PVs to monitor. This is a rare
     * use-case and is equivalent to calling addPvs with a null set of PVs.
     *
     * Allowing a listener without any PVs registered may be deprecated in the future.
     *
     * @param listener The PvListener
     */
    public void addListener(PvListener listener) {
        Set<String> pvList = listenerMap.get(listener);

        listenerMap.put(listener, pvList);
    }

    /**
     * Removes the specified listener and unregisters any PVs the listener was interested in.
     *
     * @param listener The PvListener
     */
    public void removeListener(PvListener listener) {
        //LOGGER.log(Level.FINEST, "removeListener: {0}", listener);
        Set<String> pvSet = listenerMap.get(listener);

        removeFromChannels(listener, pvSet);

        listenerMap.remove(listener);
    }

    /**
     * Returns a map of PVs to count of listeners for informational purposes.
     *
     * @return The PV to monitor map
     */
    public Map<String, Integer> getPvToCountMap() {
        Map<String, Integer> countMap = new HashMap<>();

        for (String key : monitorMap.keySet()) {
            ChannelMonitor monitor = monitorMap.get(key);
            Integer count = monitor.getListenerCount();
            countMap.put(key, count);
        }
        return countMap;
    }

    /**
     * Returns an unmodifiable map of listeners to their PVs for informational purposes.
     *
     * @return The listener to PVs map
     */
    public Map<PvListener, Set<String>> getListenerToPvsMap() {
        return Collections.unmodifiableMap(listenerMap);
    }

    /**
     * Cleans up the EPICS CA plumbing.
     *
     * @throws IOException If unable to close
     */
    @Override
    public void close() throws IOException {
        destruct();
    }
}
