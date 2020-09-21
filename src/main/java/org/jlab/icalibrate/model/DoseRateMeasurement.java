package org.jlab.icalibrate.model;

/**
 * Models a dose rate measurement or average dose rate measurement value at a specified current.
 *
 * The units for current are generally uA or nA, but it is up to the caller to keep the units
 * straight.  The reason for this is because Hall A and C use microAmps, while D uses nanoAmps.
 *
 * @author ryans
 */
public final class DoseRateMeasurement implements Comparable<DoseRateMeasurement> {

    private final double current;
    private final double doseRateRadsPerHour;

    /**
     * Creates a new DoseRateMeasurement.
     *
     * @param current The current (caller must keep up with units)
     * @param doseRateRadsPerHour The dose rate in rads per hour
     */
    public DoseRateMeasurement(double current, double doseRateRadsPerHour) {
        this.current = current;
        this.doseRateRadsPerHour = doseRateRadsPerHour;
    }

    /**
     * Return the current (units are determined by caller).
     * 
     * @return The current
     */
    public double getCurrent() {
        return current;
    }

    /**
     * Return the does rate in rads per hour.
     * 
     * @return The dose rate
     */
    public double getDoseRateRadsPerHour() {
        return doseRateRadsPerHour;
    }

    @Override
    public int compareTo(DoseRateMeasurement o) {
        Double c = current;
        return c.compareTo(o.current);
    }
}
