package org.jlab.icalibrate.model;

import java.util.List;
import org.jlab.icalibrate.epics.ChannelManager;
import org.jlab.icalibrate.exception.ValidationException;

/**
 * Models the process input parameters required to create a new hall calibration dataset.
 *
 * @author ryans
 */
public class CreateNewDatasetParameters {

  private final ChannelManager channelManager;

  private Hall hall;
  private Laser laser;
  private String target;
  private String pass;
  private String note;
  private int minAttenuator;
  private int maxAttenuator;
  private int numberOfSteps;
  private int settleSeconds;
  private int samplesPerStep;
  private List<IonChamber> icList;

  /**
   * Create a new NewDatasetParameters.
   *
   * @param channelManager The EPICS CA ChannelManager
   */
  public CreateNewDatasetParameters(ChannelManager channelManager) {
    this.channelManager = channelManager;
  }

  /**
   * Set the hall.
   *
   * @param hall The hall
   */
  public void setHall(Hall hall) {
    this.hall = hall;
  }

  /**
   * Set the laser.
   *
   * @param laser The laser
   */
  public void setLaser(Laser laser) {
    this.laser = laser;
  }

  /**
   * Set the target.
   *
   * @param target The target
   */
  public void setTarget(String target) {
    this.target = target;
  }

  /**
   * Set the pass.
   *
   * @param pass The pass
   */
  public void setPass(String pass) {
    this.pass = pass;
  }

  /**
   * Set the note.
   *
   * @param note The note
   */
  public void setNote(String note) {
    this.note = note;
  }

  /**
   * Set the minimum attenuator value.
   *
   * @param minAttenuator The minimum attenuator value (inclusive)
   */
  public void setMinAttenuator(int minAttenuator) {
    this.minAttenuator = minAttenuator;
  }

  /**
   * Set the max attenuator value.
   *
   * @param maxAttenuator The max attenuator value (inclusive)
   */
  public void setMaxAttenuator(int maxAttenuator) {
    this.maxAttenuator = maxAttenuator;
  }

  /**
   * Set the number of attenuator steps to perform.
   *
   * @param numberOfSteps The number of steps
   */
  public void setNumberOfSteps(int numberOfSteps) {
    this.numberOfSteps = numberOfSteps;
  }

  /**
   * Set the number of seconds to settle between attenuator changes.
   *
   * @param settleSeconds The number of settle seconds
   */
  public void setSettleSeconds(int settleSeconds) {
    this.settleSeconds = settleSeconds;
  }

  /**
   * Set the number of samples to collect per step.
   *
   * @param samplesPerStep The number of samples per step
   */
  public void setSamplesPerStep(int samplesPerStep) {
    this.samplesPerStep = samplesPerStep;
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
   * Return the laser.
   *
   * @return The laser
   */
  public Laser getLaser() {
    return laser;
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
   * Return the minimum attenuator value.
   *
   * @return The min attenuator value (inclusive)
   */
  public int getMinAttenuator() {
    return minAttenuator;
  }

  /**
   * Return the maximum attenuator value.
   *
   * @return The max attenuator value (inclusive)
   */
  public int getMaxAttenuator() {
    return maxAttenuator;
  }

  /**
   * Return the number of attenuator steps.
   *
   * @return The number of attenuator steps
   */
  public int getNumberOfSteps() {
    return numberOfSteps;
  }

  /**
   * Return the number of seconds to settle between attenuator changes.
   *
   * @return The settle seconds
   */
  public int getSettleSeconds() {
    return settleSeconds;
  }

  /**
   * Return the number of samples per step.
   *
   * @return The number of samples per step
   */
  public int getSamplesPerStep() {
    return samplesPerStep;
  }

  /**
   * Compute the process duration in seconds.
   *
   * @return The process duration in seconds
   */
  public int computeDurationSeconds() {
    return (settleSeconds * numberOfSteps) + (numberOfSteps * samplesPerStep);
  }

  /**
   * Compute the attenuator range.
   *
   * @return The attenuator range
   */
  public int computeAttenuatorRange() {
    return maxAttenuator - minAttenuator;
  }

  /**
   * Compute the step size. Since the attenuator is an integer we must round to the nearest whole
   * number. This means not all steps will be the same size if the goal is to include the min value
   * and the max value exactly. In particular the current algorithm assumes the last step is of
   * varying size.
   *
   * @return The step size (for all but last step, which vary to reach max attenuator exactly)
   */
  public int computeStepSize() {
    return (int) Math.round(((double) computeAttenuatorRange()) / numberOfSteps);
  }

  /**
   * Validates the option parameters and throws a ValidationException if an issue is found.
   *
   * @throws ValidationException If the option parameters do not pass validation
   */
  public void checkOptionValidity() throws ValidationException {
    if (minAttenuator < 0) {
      throw new ValidationException("Min Attenuator can not be negative");
    }

    if (minAttenuator > maxAttenuator) {
      throw new ValidationException("Min Attenuator can not be greater than Max Attenuator");
    }

    if (numberOfSteps < 1) {
      throw new ValidationException("# of Attenuator Steps must be more than 0");
    }

    if (computeAttenuatorRange() < numberOfSteps) {
      throw new ValidationException("Number of Attenuator steps is greater than attenuator range");
    }

    if (settleSeconds < 1) {
      throw new ValidationException("Step Settle Time (Seconds) must be more than 0");
    }

    if (samplesPerStep < 1) {
      throw new ValidationException("# of Samples Per Step must be more than 0");
    }
  }

  /**
   * Return the channel manager.
   *
   * @return The channel manager
   */
  public ChannelManager getChannelManager() {
    return channelManager;
  }

  /**
   * Sets the list of ion chambers.
   *
   * @param icList The ion chambers
   */
  public void setIonChamberList(List<IonChamber> icList) {
    this.icList = icList;
  }

  /**
   * Return the list of ion chambers.
   *
   * @return The ion chambers
   */
  public List<IonChamber> getIonChamberList() {
    return icList;
  }
}
