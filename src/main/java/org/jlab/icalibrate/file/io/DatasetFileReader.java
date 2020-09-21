package org.jlab.icalibrate.file.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;
import org.jlab.icalibrate.exception.MissingDataException;
import org.jlab.icalibrate.model.DoseRateMeasurement;
import org.jlab.icalibrate.model.Hall;
import org.jlab.icalibrate.model.HallCalibrationDataset;
import org.jlab.icalibrate.model.IonChamber;
import org.jlab.icalibrate.model.IonChamberDataset;

/**
 * Responsible for reading hall calibration dataset (HCD) files.
 *
 * @author ryans
 */
public class DatasetFileReader {

    private static final Logger LOGGER = Logger.getLogger(
            DatasetFileReader.class.getName());

    /**
     * Reads the specified file and returns the HallCalibrationDataset.
     *
     * @param file The HCD file
     * @return The parsed HallCalibrationDataset
     * @throws FileNotFoundException If the file is not found
     * @throws IOException If an IO problem occurs while reading or parsing
     * @throws ParseException If a problem occurs parsing data in the file
     * @throws MissingDataException If expected data is not found
     */
    public HallCalibrationDataset read(File file) throws FileNotFoundException, IOException,
            ParseException, MissingDataException {

        Header header;
        List<IonChamberDataset> datasetList = new ArrayList<>();

        try (Scanner scanner = new Scanner(file)) {
            header = parseHeader(scanner);
            if (header.fileFormat == null) {
                parseBodyMatrix(scanner, datasetList, header.hall);
            } else {
                parseBodyIndependentICs(scanner, datasetList, header.hall);
            }

        }

        return new HallCalibrationDataset(header.hall, header.target, header.pass, header.note, header.calibratedDate,
                header.calibratedBy, datasetList);
    }

    private Header parseHeader(Scanner scanner) throws IOException, ParseException {
        Header header = new Header();

        // START
        String line = scanner.nextLine();
        if (!"--- Start HCD header".equals(line)) {
            throw new IOException("HCD file is missing header");
        }

        // Hall
        line = scanner.nextLine();
        String[] tokens = line.split(":");
        if (!"Hall".equals(tokens[0])) {
            throw new IOException("HCD file is missing Hall metadata");
        }
        String hall = tokens[1].trim();
        header.hall = Hall.valueOf(hall);

        // Target
        line = scanner.nextLine();
        tokens = line.split(":");
        if (!"Target".equals(tokens[0])) {
            throw new IOException("HCD file is missing Target metadata");
        }
        String target = line.substring(tokens[0].length() + 1);
        header.target = target;

        // Pass
        line = scanner.nextLine();
        tokens = line.split(":");
        if (!"Pass".equals(tokens[0])) {
            throw new IOException("HCD file is missing Pass metadata");
        }
        String pass = line.substring(tokens[0].length() + 1);
        header.pass = pass;

        // Note
        line = scanner.nextLine();
        tokens = line.split(":");
        if (!"Note".equals(tokens[0])) {
            throw new IOException("HCD file is missing Note metadata");
        }
        String note = line.substring(tokens[0].length() + 1);
        header.note = note;

        // Calibrated Date
        line = scanner.nextLine();
        tokens = line.split(":");
        if (!"Calibrated Date".equals(tokens[0])) {
            throw new IOException("HCD file is missing Calibrated Date metadata");
        }
        String calibratedDate = line.substring(tokens[0].length() + 1);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        header.calibratedDate = formatter.parse(calibratedDate);

        // Calibrated By
        line = scanner.nextLine();
        tokens = line.split(":");
        if (!"Calibrated By".equals(tokens[0])) {
            throw new IOException("HCD file is missing Calibrated By metadata");
        }
        String calibratedBy = line.substring(tokens[0].length() + 1);
        header.calibratedBy = calibratedBy;

        // END
        line = scanner.nextLine();

        tokens = line.split(":");
        if ("File Format".equals(tokens[0])) {
            header.fileFormat = "Independent ICs";
            line = scanner.nextLine();
        }

        if (!"--- End HCD header".equals(line)) {
            throw new IOException("HCD file header is longer than expected");
        }

        return header;
    }

    private void parseBodyMatrix(Scanner scanner, List<IonChamberDataset> datasetList, Hall hall) throws
            MissingDataException {
        String line = scanner.nextLine();

        String[] tokens = line.split(",");

        List<String> ionChamberList = new ArrayList<>();
        List<Double> currentList = new ArrayList<>();
        List<List<Double>> doseListMatrix = new ArrayList<>();

        for (int i = 1; i < tokens.length; i++) {
            ionChamberList.add(tokens[i].trim());
        }

        while (scanner.hasNextLine()) {
            line = scanner.nextLine();

            tokens = line.split(",");

            Double current = Double.parseDouble(tokens[0].trim());
            currentList.add(current);

            List<Double> doseList = new ArrayList<>();
            doseListMatrix.add(doseList);

            for (int i = 1; i < tokens.length; i++) {
                String token = tokens[i].trim();
                Double value = Double.parseDouble(token);
                doseList.add(value);
            }
        }

        for (int i = 0; i < ionChamberList.size(); i++) {
            String epicsName = ionChamberList.get(i);
            String friendlyName = null;
            
            IonChamber ic = IonChamber.newInstance(hall, epicsName, friendlyName, null);

            List<DoseRateMeasurement> measurementList = new ArrayList<>();

            for (int j = 0; j < doseListMatrix.size(); j++) {
                List<Double> doseList = doseListMatrix.get(j);
                Double current = currentList.get(j);
                Double doseRate = doseList.get(i);

                measurementList.add(new DoseRateMeasurement(current, doseRate));
            }

            datasetList.add(new IonChamberDataset(ic, measurementList));
        }
    }

    private void parseBodyIndependentICs(Scanner scanner, List<IonChamberDataset> datasetList, Hall hall) throws MissingDataException {
        String line;
        String[] tokens;
        IonChamber ic = null;
        List<DoseRateMeasurement> measurementList = null;
        double current;
        double doseRateRadsPerHour;

        do {
            line = scanner.nextLine();
            
            tokens = line.split(" ");

            if ("#IC:".equals(tokens[0])) {
                if (ic != null) {
                    datasetList.add(new IonChamberDataset(ic, measurementList));
                }

                String epicsName = tokens[1];
                String friendlyName = null;
                
                String[] pieces = line.split(epicsName);
                if(pieces.length == 2) {
                    friendlyName = pieces[1].trim();
                }
                
                ic = IonChamber.newInstance(hall, epicsName, friendlyName, null);
                measurementList = new ArrayList<>();
            } else {
                current = Double.parseDouble(tokens[0]);
                doseRateRadsPerHour = Double.parseDouble(tokens[1]);
                measurementList.add(new DoseRateMeasurement(current, doseRateRadsPerHour));
            }
        } while (scanner.hasNextLine());

        if (ic != null) {
            datasetList.add(new IonChamberDataset(ic, measurementList));
        }
    }

    private class Header {

        public Hall hall;
        public String target;
        public String pass;
        public String note;
        public Date calibratedDate;
        public String calibratedBy;
        public String fileFormat;
    }
}
