package org.jlab.icalibrate.model;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.List;
import java.util.Objects;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.jlab.icalibrate.ICalibrateApp;
import org.jlab.icalibrate.IOUtil;
import org.jlab.icalibrate.exception.MissingDataException;

/**
 * Models and ion chamber and all of the metadata associated with it.
 *
 * @author ryans
 */
public final class IonChamber implements Comparable<IonChamber> {

    private static final Logger LOGGER = Logger.getLogger(
            IonChamber.class.getName());

    private final String epicsName;
    private final String cedName;
    private final String friendlyName;
    private final String doseRateReadPvName;
    private final String doseRateSetpointWritePvName;
    private final String doseRateSetpointReadPvName;
    private final String idPvName;

    /**
     * Create a new Ion Chamber.
     *
     * @param epicsName The epicsName of the ion chamber device
     * @param cedName The CED name
     * @param friendlyName The user friendly name
     * @param doseRateReadPvName The EPICS PV epicsName for reading the device's
     * dose rate in rads per hour
     * @param doseRateSetpointReadPvName The EPICS PV epicsName for reading the
     * device's dose rate trip setpoint in rads per hour
     * @param doseRateSetpointWritePvName The EPICS PV epicsName for writing the
     * device's dose rate trip setpoint in rads per hour
     * @param idPvName The EPICS PV epicsName for the device's identifier /
     * channel
     */
    public IonChamber(String epicsName, String cedName, String friendlyName,
            String doseRateReadPvName, String doseRateSetpointReadPvName,
            String doseRateSetpointWritePvName, String idPvName) {
        this.epicsName = epicsName;
        this.cedName = cedName;
        this.friendlyName = friendlyName;
        this.doseRateReadPvName = doseRateReadPvName;
        this.doseRateSetpointReadPvName = doseRateSetpointReadPvName;
        this.doseRateSetpointWritePvName = doseRateSetpointWritePvName;
        this.idPvName = idPvName;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(this.epicsName);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IonChamber other = (IonChamber) obj;
        if (!Objects.equals(this.epicsName, other.epicsName)) {
            return false;
        }
        return true;
    }  
    
    @Override
    public int compareTo(IonChamber other) {
        return this.getFriendlyNameOrEpicsName().compareTo(other.getFriendlyNameOrEpicsName());
    }
    
    /**
     * Return the EPICS name.
     *
     * @return The EPICS name
     */
    public String getEpicsName() {
        return epicsName;
    }

    /**
     * Return the CED name.
     *
     * @return The CED name
     */
    public String getCedName() {
        return cedName;
    }

    /**
     * Return the friendly name (responsible group assigned common name).
     *
     * @return The friendly name
     */
    public String getFriendlyName() {
        return friendlyName;
    }

    /**
     * Return the friendly name, or if unavailable, return the EPICS name.
     *
     * @return The friendly name, or the EPICS name
     */
    public String getFriendlyNameOrEpicsName() {
        String name;

        if (friendlyName == null) {
            name = epicsName;
        } else {
            name = friendlyName;
        }

        return name;
    }

    /**
     * Return the full name which is a combination of all three names (friendly,
     * EPICS, CED).
     *
     * @return The full name
     */
    public String getFullName() {
        String name;

        if (friendlyName == null) {
            name = epicsName;
        } else if (cedName == null) {
            name = friendlyName + " (" + epicsName + ")";
        } else {
            name = friendlyName + " (" + epicsName + ")" + " {" + cedName + "}";
        }

        return name;
    }

    /**
     * Return the dose rate read PV name.
     *
     * @return The PV name
     */
    public String getDoseRateReadPvName() {
        return doseRateReadPvName;
    }

    /**
     * Return the dose rate setpoint read PV name.
     *
     * @return The PV name
     */
    public String getDoseRateSetpointReadPvName() {
        return doseRateSetpointReadPvName;
    }

    /**
     * Return the dose rate setpoint write PV name.
     *
     * @return The PV name
     */
    public String getDoseRateSetpointWritePvName() {
        return doseRateSetpointWritePvName;
    }

    /**
     * Return the identifier PV name.
     *
     * @return PV name
     */
    public String getIdPvName() {
        return idPvName;
    }

    /**
     * Creates the list of ion chambers for a hall using either the CED or properties file.
     *
     * @param hall The experimental hall
     * @return The list of ion chambers
     * @throws MissingDataException If the data format is unrecognizable
     * @throws MalformedURLException If the remote server URL is malformed
     * @throws ProtocolException If unable to communicate with the server
     * @throws IOException If an I/O problem occurs
     */
    public static List<IonChamber> createIcs(Hall hall) throws MissingDataException, MalformedURLException, ProtocolException, IOException {
        if ("true".equals(ICalibrateApp.APP_PROPERTIES.getProperty("NAMES_FROM_CED"))) {
            return createFromCed(hall);
        } else {
            return createIcsFromConfig(hall);
        }
    }

    /**
     * Create a new list of hall IonChambers from the CEBAF Element Database (CED).
     *
     * @param hall The experimental hall
     * @return The list of ion chambers
     * @throws MissingDataException If the JSON data format is unrecognizable
     * @throws MalformedURLException If the remote server URL is malformed
     * @throws ProtocolException If unable to communicate with the server
     * @throws IOException If an I/O problem occurs
     */
    public static List<IonChamber> createFromCed(Hall hall) throws MissingDataException, MalformedURLException, ProtocolException, IOException {
        List<IonChamber> icList = new ArrayList<>();

        String url = "http://ced.acc.jlab.org/inventory?t=IonChamber&Ex=iCalibrate%3D1&a=A_Hall" + hall.name() + "&p=EPICSName%2C+NameAlias&out=json";

        String jsonStr = IOUtil.doHttpGet(url, 10000, 10000);

        JsonReader reader = Json.createReader(new StringReader(jsonStr));

        JsonObject jsonObj = reader.readObject();

        JsonObject inventory = jsonObj.getJsonObject("Inventory");
        JsonArray elements = inventory.getJsonArray("elements");

        for (int i = 0; i < elements.size(); i++) {
            JsonObject value = elements.getJsonObject(i);
            JsonObject properties = value.getJsonObject("properties");
            
            String epicsName = properties.getString("EPICSName");
            String friendlyName = properties.getString("NameAlias");
            String cedName = value.getString("name");

            if (epicsName.equals(cedName)) {
                cedName = null;
            }

            IonChamber ic = newInstance(hall, epicsName, friendlyName, cedName);
            icList.add(ic);
        }

        return icList;
    }

    /**
     * Create a new list of ion chambers from the icalibrate.properties file.
     *
     * @param hall The experimental hall
     * @return The list of ion chambers
     * @throws MissingDataException If the properties file format is unrecognizable
     */
    public static List<IonChamber> createIcsFromConfig(Hall hall) throws MissingDataException {

        List<IonChamber> icList = new ArrayList<>();

        String hallPrefix = "HALLA";

        if (hall == Hall.C) {
            hallPrefix = "HALLC";
        } else if (hall == Hall.D) {
            hallPrefix = "HALLD";
        }

        String epicsNamesProperty = hallPrefix + "_EPICS_NAME_CSV";
        String codedNamesProperty = hallPrefix + "_CED_NAME_CSV";
        String friendlyNamesProperty = hallPrefix + "_FRIENDLY_NAME_CSV";

        String[] epicsTokens = null;
        String[] cedTokens = null;
        String[] friendlyTokens = null;

        try {
            epicsTokens
                    = ICalibrateApp.APP_PROPERTIES.getProperty(epicsNamesProperty).split(
                            ",");
            cedTokens = ICalibrateApp.APP_PROPERTIES.getProperty(codedNamesProperty).split(
                    ",");
            friendlyTokens
                    = ICalibrateApp.APP_PROPERTIES.getProperty(friendlyNamesProperty).split(",");
        } catch (NullPointerException e) {
            throw new MissingDataException("Name CSV missing from config", e);
        }

        String cedName = null;
        String friendlyName;

        if (epicsTokens != null && cedTokens != null && friendlyTokens != null) {
            if (epicsTokens.length == cedTokens.length && cedTokens.length == friendlyTokens.length) {
                for (int i = 0; i < epicsTokens.length; i++) {
                    String epicsName = epicsTokens[i];
                    //cedName = cedTokens[i];
                    friendlyName = friendlyTokens[i];
                    IonChamber ic = newInstance(hall, epicsName, friendlyName, cedName);
                    icList.add(ic);
                }
            } else {
                throw new MissingDataException("EPICS, CED, or Friendly Name List incomplete");
            }
        } else {
            throw new MissingDataException("EPICS, CED, or Friendly Name List missing");
        }

        return icList;
    }

    /**
     * Create a new instance of an ion chamber given the hall and EPICS name.
     * The rest of the metadata is loaded from configuration.
     *
     * @param hall The hall
     * @param epicsName The EPICS name
     * @param friendlyName The friendly name or null
     * @param cedName The CED name or null
     * @return A new ion chamber
     * @throws MissingDataException If the configuration file is missing
     * required data
     */
    public static IonChamber newInstance(Hall hall, String epicsName, String friendlyName, String cedName) throws MissingDataException {

        String hallPrefix = "HALLA";

        if (hall == Hall.C) {
            hallPrefix = "HALLC";
        } else if (hall == Hall.D) {
            hallPrefix = "HALLD";
        }

        String doseReadSuffix = hallPrefix + "_DOSE_READ_PV_SUFFIX";
        String readSetpointSuffix = hallPrefix + "_DOSE_SETPOINT_READ_PV_SUFFIX";
        String writeSetpointSuffix = hallPrefix + "_DOSE_SETPOINT_WRITE_PV_SUFFIX";

        String doseRateReadbackPvName = epicsName + ICalibrateApp.APP_PROPERTIES.getProperty(
                doseReadSuffix);
        String doseRateSetpointReadPvName = epicsName + ICalibrateApp.APP_PROPERTIES.getProperty(
                readSetpointSuffix);
        String doseRateSetpointWritePvName = epicsName + ICalibrateApp.APP_PROPERTIES.getProperty(
                writeSetpointSuffix);
        String idPvName = "";

        return new IonChamber(epicsName, cedName, friendlyName, doseRateReadbackPvName,
                doseRateSetpointReadPvName,
                doseRateSetpointWritePvName, idPvName);
    }
}
