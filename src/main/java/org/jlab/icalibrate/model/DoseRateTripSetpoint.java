package org.jlab.icalibrate.model;

/**
 * Models the dose rate setpoint for a given ion chamber.
 * 
 * @author ryans
 */
public final class DoseRateTripSetpoint {
    private final IonChamber ionChamber;
    private final double doseRateRadsPerHour;

    /**
     * Create a new DoseRateTripSetpoint for a given ion chamber.
     * 
     * @param ionChamber The ion chamber
     * @param doseRateRadsPerHour The dose rate setpoint
     */
    public DoseRateTripSetpoint(IonChamber ionChamber, double doseRateRadsPerHour) {
        this.ionChamber = ionChamber;
        this.doseRateRadsPerHour = doseRateRadsPerHour;
    }

    /**
     * Return the ion chamber.
     * 
     * @return The ion chamber
     */
    public IonChamber getIonChamber() {
        return ionChamber;
    }

    /**
     * Return the dose rate setpoint in rads per hour.
     * 
     * @return The dose rate setpoint
     */
    public double getDoseRateRadsPerHour() {
        return doseRateRadsPerHour;
    }
}
