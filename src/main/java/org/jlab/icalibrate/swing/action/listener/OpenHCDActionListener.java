package org.jlab.icalibrate.swing.action.listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jlab.icalibrate.ICalibrateApp;
import org.jlab.icalibrate.exception.MissingDataException;
import org.jlab.icalibrate.file.io.DatasetFileReader;
import org.jlab.icalibrate.model.HallCalibrationDataset;
import org.jlab.icalibrate.swing.generated.ICalibrateFrame;
import org.jlab.icalibrate.swing.worker.MinimumExecutionSwingWorker;

/**
 * Handle an open Hall Calibration Data file request.
 *
 * @author ryans
 */
public final class OpenHCDActionListener implements ActionListener {

  private static final Logger LOGGER = Logger.getLogger(OpenHCDActionListener.class.getName());

  private final ICalibrateFrame frame;

  /**
   * Create a new OpenHCDActionListener.
   *
   * @param frame The ICalibrateFrame
   */
  public OpenHCDActionListener(ICalibrateFrame frame) {
    this.frame = frame;
  }

  @Override
  public void actionPerformed(ActionEvent e) {

    String dir = ICalibrateApp.APP_PROPERTIES.getProperty("DEFAULT_HCD_FILE_DIR");

    JFileChooser openDatasetFileChooser = new JFileChooser();

    openDatasetFileChooser.setDialogTitle("Choose Hall Calibration Dataset File");
    openDatasetFileChooser.setFileFilter(
        new FileNameExtensionFilter("Hall Calibration Dataset (*.hcd)", "hcd"));

    if (dir != null) {
      openDatasetFileChooser.setCurrentDirectory(new File(dir));
    }

    int returnVal = openDatasetFileChooser.showOpenDialog(frame);
    if (returnVal == JFileChooser.APPROVE_OPTION) {

      frame.closeHallCalibrationDataset();

      File file = openDatasetFileChooser.getSelectedFile();

      frame.queueShowModalWait();
      new MinimumExecutionSwingWorker<HallCalibrationDataset, Void>() {

        @Override
        protected HallCalibrationDataset doWithMinimumExecution() throws Exception {
          DatasetFileReader reader = new DatasetFileReader();
          HallCalibrationDataset ds = reader.read(file);

          return ds;
        }

        @Override
        protected void done() {
          try {
            HallCalibrationDataset ds = get(); // See if there were any exceptions
            frame.setDataset(ds, file.getName(), true);
          } catch (InterruptedException | ExecutionException ex) {
            String title = "Unable to open file";
            String message = "Unexpected error";
            LOGGER.log(Level.SEVERE, title, ex);

            Throwable cause = ex.getCause();
            if (cause != null && cause instanceof MissingDataException) {
              message =
                  "Current Ion Chamber configuration (names) do not match data file: "
                      + ex.getMessage();
            }

            JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE);
          } finally {
            frame.hideModalWait();
          }
        }
      }.execute();
    }
  }
}
