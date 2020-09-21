package org.jlab.icalibrate.model;

import java.util.Collections;
import java.util.List;

/**
 * Models the measured dose rate data for a given ion chamber.
 * 
 * @author ryans
 */
public final class IonChamberDataset {
    private final IonChamber ionChamber;
    private final List<DoseRateMeasurement> measurementList;

    /**
     * Create a new IonChamberDoseRateSample.
     * 
     * @param ionChamber The ion chamber
     * @param measurementList The measured data
     */
    public IonChamberDataset(IonChamber ionChamber, List<DoseRateMeasurement> measurementList) {
        this.ionChamber = ionChamber;
        this.measurementList = measurementList;
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
     * Return the measured dose rate list.
     * 
     * @return The measured dose rate list
     */
    public List<DoseRateMeasurement> getMeasurementList() {
        return Collections.unmodifiableList(measurementList);
    }
}
