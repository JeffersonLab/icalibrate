package org.jlab.icalibrate.swing.worker;

import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.dbr.DBR;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.jlab.icalibrate.ICalibrateApp;
import org.jlab.icalibrate.epics.ChannelManager;
import org.jlab.icalibrate.exception.AppException;
import org.jlab.icalibrate.model.CreateNewDatasetParameters;
import org.jlab.icalibrate.model.Hall;
import org.jlab.icalibrate.swing.generated.wizard.Wizard;
import org.jlab.icalibrate.swing.generated.wizard.page.LaserTargetBeamPage;

/**
 * This is a SwingWorker which looks up the pass in use for the selected hall. The MMS PVs are
 * consulted to determine the pass. Once the worker is complete it decrements the count down latch
 * such that the parent thread can determine when all concurrently executing child threads are
 * complete. The information gained from this worker is displayed on the LaserTargetBeamPage.
 *
 * @author ryans
 */
public class PassLookupWorker extends MinimumExecutionSwingWorker<String, Void> {

  private static final Logger LOGGER = Logger.getLogger(PassLookupWorker.class.getName());

  private final Wizard<CreateNewDatasetParameters> wizard;
  private final LaserTargetBeamPage page;
  private final CountDownLatch doneLatch;

  /**
   * Create a new PassLookupWorker.
   *
   * @param wizard The wizard
   * @param page The page
   * @param doneLatch The latch
   */
  public PassLookupWorker(
      Wizard<CreateNewDatasetParameters> wizard,
      LaserTargetBeamPage page,
      CountDownLatch doneLatch) {
    this.wizard = wizard;
    this.page = page;
    this.doneLatch = doneLatch;
  }

  @Override
  protected String doWithMinimumExecution() throws Exception {
    String pass = null;

    CreateNewDatasetParameters params = wizard.getParameters();

    Hall hall = params.getHall();

    String hallPrefix;

    switch (hall) {
      case A:
        hallPrefix = "HALLA_";
        break;
      case C:
        hallPrefix = "HALLC_";
        break;
      case D:
        hallPrefix = "HALLD_";
        break;
      default:
        throw new IllegalArgumentException("Hall must be one of A, C, D");
    }

    String passPv = ICalibrateApp.APP_PROPERTIES.getProperty(hallPrefix + "PASS_READ_PV");

    if (passPv != null) {
      passPv = passPv.trim(); // Config file might have spaces!
    }

    ChannelManager manager = params.getChannelManager();

    DBR passValue;

    try {
      passValue = manager.get(passPv);
    } catch (IllegalStateException e) {
      throw new AppException(
          "Channel Access disconnected during EPICS CA Get of PV: "
              + passPv
              + " - "
              + e.getMessage(),
          e);
    } catch (CAException e) {
      throw new AppException(
          "Unable to perform EPICS CA Get of PV: " + passPv + " - " + e.getMessage(), e);
    } catch (TimeoutException e) {
      throw new AppException("Timeout waiting for EPICS CA Get of PV: " + passPv, e);
    }

    int passNum = ((gov.aps.jca.dbr.DBR_Enum) passValue).getEnumValue()[0];

    if (hall == Hall.D) {
      switch (passNum) {
        case 1:
          pass = "0.5";
          break;
        case 2:
          pass = "5.5";
          break;
        default:
          throw new AppException("Expected Hall D Pass Enum to contain value 1 or 2");
      }
    } else {
      pass = String.valueOf(passNum);
    }

    LOGGER.log(Level.FINEST, "Pass: {0}", pass);

    return pass;
  }

  @Override
  protected void done() {
    try {
      String pass = get(); // See if there were any exceptions
      page.setPass(pass);
    } catch (InterruptedException | ExecutionException ex) {
      page.setPass("Unknown");
      String title = "Unable to lookup hall pass configuration";
      String message = "Unable to lookup pass in EPICS";
      LOGGER.log(Level.SEVERE, title, ex);

      Throwable cause = ex.getCause();
      if (cause != null) {
        if (cause instanceof AppException) {
          message = cause.getMessage();
        } else if (cause instanceof CAException) {
          message = cause.getMessage();
        } else if (cause instanceof TimeoutException) {
          message = "Timeout waiting for response from EPICS";
        }
      }
      JOptionPane.showMessageDialog(wizard, message, title, JOptionPane.ERROR_MESSAGE);
    } finally {
      doneLatch.countDown();
    }
  }
}
