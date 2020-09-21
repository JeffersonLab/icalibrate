package org.jlab.icalibrate.file.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.jlab.icalibrate.model.DoseRateTripSetpoint;

/**
 * Responsible for writing SNAP files.
 * 
 * @author ryans
 */
public class SnapFileWriter {

    /**
     * Write the list of DoseRateTripSetpoints to the specified SNAP file.
     * 
     * @param file The SNAP file
     * @param setpointList The list of DoseRateTripSetpoints
     * @throws FileNotFoundException If the file path is invalid
     * @throws UnsupportedEncodingException If unable to encode the file in UTF-8
     */
    public void write(File file, List<DoseRateTripSetpoint> setpointList) throws FileNotFoundException, UnsupportedEncodingException {
        try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
            writeHeader(writer);
            writePvs(writer, setpointList);
        }
    }
    
    private void writeHeader(PrintWriter writer) {
        SimpleDateFormat formatter = new SimpleDateFormat("E MMM d HH:mm:ss yyyy");
        
        writer.println("--- Start BURT header");
        writer.println("Time: " + formatter.format(new Date()));
        writer.println("Login ID: " + System.getProperty("user.name"));
        writer.println("Eff UID: ");
        writer.println("Group ID: ");
        writer.println("Keywords: ");
        writer.println("Comments: ");
        writer.println("Type: ");
        writer.println("Directory ");
        writer.println("Req File: ");
        writer.println("--- End BURT header");
    }
    
    private void writePvs(PrintWriter writer, List<DoseRateTripSetpoint> setpointList) {
        DecimalFormat formatter = new DecimalFormat("0");
        for(DoseRateTripSetpoint setpoint: setpointList) {
            String pvName = setpoint.getIonChamber().getDoseRateSetpointWritePvName();
            writer.println(pvName + " 1 " + formatter.format(setpoint.getDoseRateRadsPerHour()));
        }
    }
}
