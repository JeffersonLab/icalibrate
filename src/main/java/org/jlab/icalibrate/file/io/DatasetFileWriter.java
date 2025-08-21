package org.jlab.icalibrate.file.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.jlab.icalibrate.model.DoseRateMeasurement;
import org.jlab.icalibrate.model.Hall;
import org.jlab.icalibrate.model.HallCalibrationDataset;
import org.jlab.icalibrate.model.IonChamberDataset;

/**
 * Responsible for writing hall calibration dataset (HCD) files.
 *
 * @author ryans
 */
public class DatasetFileWriter {

  /** Create a new DatasetFileWriter. */
  public DatasetFileWriter() {}

  /**
   * Writes the HallCalibrationDataset to the specified file.
   *
   * @param file The file to write to
   * @param dataset The dataset to write
   * @throws FileNotFoundException If the file path is invalid
   * @throws UnsupportedEncodingException If unable to encode in UTF-8
   */
  public void write(File file, HallCalibrationDataset dataset)
      throws FileNotFoundException, UnsupportedEncodingException {
    try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
      writeHeader(
          writer,
          dataset.getHall(),
          dataset.getTarget(),
          dataset.getPass(),
          dataset.getNote(),
          dataset.getCalibratedDate(),
          dataset.getCalibratedBy());
      writeBodyIndependentICs(writer, dataset.getMeasuredDoseRateDataset());
    }
  }

  private void writeHeader(
      PrintWriter writer,
      Hall hall,
      String target,
      String pass,
      String note,
      Date calibratedDate,
      String calibratedBy) {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

    writer.println("--- Start HCD header");
    writer.println("Hall: " + hall.name());
    writer.println("Target: " + target);
    writer.println("Pass: " + pass);
    writer.println("Note: " + note);
    writer.println("Calibrated Date: " + formatter.format(calibratedDate));
    writer.println("Calibrated By: " + calibratedBy);
    writer.println("File Format: Independent ICs");
    writer.println("--- End HCD header");
  }

  private void writeBodyIndependentICs(PrintWriter writer, List<IonChamberDataset> sampleList) {
    DecimalFormat currentFormatter = new DecimalFormat("0.00");
    DecimalFormat doseRateFormatter = new DecimalFormat("0");

    if (sampleList != null) {
      for (IonChamberDataset sample : sampleList) {
        String epicsName = sample.getIonChamber().getEpicsName();
        String friendlyName = sample.getIonChamber().getFriendlyName();
        writer.println("#IC: " + epicsName + " " + friendlyName);
        List<DoseRateMeasurement> measurementList = sample.getMeasurementList();

        if (measurementList != null) {
          for (int i = 0; i < measurementList.size(); i++) {
            DoseRateMeasurement measurement = measurementList.get(i);

            writer.print(currentFormatter.format(measurement.getCurrent()));
            writer.print(" ");
            writer.println(doseRateFormatter.format(measurement.getDoseRateRadsPerHour()));
          }
        }
      }
    }
  }

  private void writeBodyMatrix(PrintWriter writer, List<IonChamberDataset> sampleList) {
    if (sampleList != null) {
      List<String> icNameList = new ArrayList<>();
      List<List<Double>> matrix = new ArrayList<>();

      icNameList.add("#HALL CURR");

      boolean firstPass = true;

      for (IonChamberDataset sample : sampleList) {
        String name = sample.getIonChamber().getEpicsName();
        icNameList.add(name);
        List<DoseRateMeasurement> measurementList = sample.getMeasurementList();

        if (measurementList != null) {
          for (int i = 0; i < measurementList.size(); i++) {
            DoseRateMeasurement measurement = measurementList.get(i);
            List<Double> row;

            if (firstPass) {
              row = new ArrayList<>();
              row.add(measurement.getCurrent());
              matrix.add(row);
            } else {
              row = matrix.get(i);
            }

            row.add(measurement.getDoseRateRadsPerHour());
          }
        }

        firstPass = false;
      }

      writeBodyMatrix(writer, icNameList, matrix);
    }
  }

  private void writeBodyMatrix(
      PrintWriter writer, List<String> icNameList, List<List<Double>> matrix) {
    DecimalFormat currentFormatter = new DecimalFormat("0.00");
    DecimalFormat doseRateFormatter = new DecimalFormat("0");

    if (icNameList != null && !icNameList.isEmpty()) {
      writer.print(icNameList.get(0));

      for (int i = 1; i < icNameList.size(); i++) {
        writer.print(",");
        writer.print(icNameList.get(i));
      }

      writer.println();

      if (matrix != null) {
        for (List<Double> row : matrix) {
          if (row != null && !row.isEmpty()) {
            writer.print(currentFormatter.format(row.get(0)));

            for (int i = 1; i < row.size(); i++) {
              writer.print(",");
              writer.print(doseRateFormatter.format(row.get(i)));
            }

            writer.println();
          }
        }
      }
    }
  }
}
