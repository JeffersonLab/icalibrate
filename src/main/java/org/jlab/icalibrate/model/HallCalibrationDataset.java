package org.jlab.icalibrate.model;

import java.util.Date;
import java.util.List;

/**
 * Models a hall calibration dataset with all metadata.
 *
 * @author ryans
 */
public class HallCalibrationDataset {
  private final Hall hall;
  private final String target;
  private final String pass;
  private final String note;
  private final Date calibratedDate;
  private final String calibratedBy;
  private final List<IonChamberDataset> datasetList;

  /**
   * Create a new HallCalibrationDataset.
   *
   * @param hall The hall
   * @param target The target
   * @param pass The pass
   * @param note The note about special circumstances
   * @param calibratedDate The date of calibration
   * @param calibratedBy The username of the operator whom performed the calibration
   * @param datasetList The list of measured dose rate data
   */
  public HallCalibrationDataset(
      Hall hall,
      String target,
      String pass,
      String note,
      Date calibratedDate,
      String calibratedBy,
      List<IonChamberDataset> datasetList) {
    this.hall = hall;
    this.target = target;
    this.pass = pass;
    this.note = note;
    this.calibratedDate = calibratedDate;
    this.calibratedBy = calibratedBy;
    this.datasetList = datasetList;
  }

  /**
   * Return the hall.
   *
   * @return The hall
   */
  public Hall getHall() {
    return hall;
  }

  /**
   * Return the target.
   *
   * @return The target
   */
  public String getTarget() {
    return target;
  }

  /**
   * Return the pass.
   *
   * @return The pass
   */
  public String getPass() {
    return pass;
  }

  /**
   * Return the note.
   *
   * @return The note
   */
  public String getNote() {
    return note;
  }

  /**
   * Return the measured dose rate dataset.
   *
   * @return The sample data
   */
  public List<IonChamberDataset> getMeasuredDoseRateDataset() {
    return datasetList;
  }

  /**
   * Return the date of calibration.
   *
   * @return The calibration date
   */
  public Date getCalibratedDate() {
    return calibratedDate;
  }

  /**
   * Return the username of the operator whom performed the calibration.
   *
   * @return The username of the operator whom performed the calibration
   */
  public String getCalibratedBy() {
    return calibratedBy;
  }
}
