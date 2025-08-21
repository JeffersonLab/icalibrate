package org.jlab.icalibrate.swing.action;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jlab.icalibrate.file.io.SnapFileWriter;
import org.jlab.icalibrate.model.DoseRateTripSetpoint;
import org.jlab.icalibrate.swing.chooser.ConfirmOverwriteFileChooser;
import org.jlab.icalibrate.swing.generated.ICalibrateFrame;
import org.jlab.icalibrate.swing.worker.MinimumExecutionSwingWorker;

/**
 * Handle an export snap file request.
 *
 * <p>This class is an Action and not simply an ActionListener because the label (name) is used to
 * set the ChooseAndModifySetpointDialog button label.
 *
 * @author ryans
 */
public final class ExportSnapAction extends AbstractAction {

  private static final Logger LOGGER = Logger.getLogger(ExportSnapAction.class.getName());

  /** The frame. */
  private final ICalibrateFrame frame;

  /**
   * Create a new ExportExcelAction.
   *
   * @param frame The ICalibrateFrame
   */
  public ExportSnapAction(ICalibrateFrame frame) {
    this.frame = frame;

    putValue(AbstractAction.NAME, "Write to SNAP File");
  }

  @Override
  public void actionPerformed(ActionEvent e) {

    frame.getModifySetpointDialog().setVisible(false);

    String title = frame.getFilename();

    if (title != null && title.contains(".")) {
      title = title.substring(0, title.lastIndexOf("."));
    } else {
      title = "iCalibrate";
    }

    ConfirmOverwriteFileChooser saveSnapFileChooser = new ConfirmOverwriteFileChooser();

    saveSnapFileChooser.setDialogTitle("Choose Snap File");
    // saveFileChooser.setFileFilter(new SnapFileFilter());
    // saveSnapFileChooser.setSelectedFile(new File("icalibrate.snap"));
    saveSnapFileChooser.setFileFilter(new FileNameExtensionFilter("Snap File (*.snap)", "snap"));

    saveSnapFileChooser.setSelectedFile(new File(title + ".snap"));
    int returnVal = saveSnapFileChooser.showSaveDialog(frame);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File file = saveSnapFileChooser.getSelectedFile();

      frame.queueShowModalWait();
      new MinimumExecutionSwingWorker<Void, Void>() {

        @Override
        protected Void doWithMinimumExecution() throws Exception {
          SnapFileWriter writer = new SnapFileWriter();

          List<DoseRateTripSetpoint> setpointList = frame.getModifySetpointDialog().getSetpoints();

          writer.write(file, setpointList);

          return null;
        }

        @Override
        protected void done() {
          try {
            get(); // See if there were any exceptions
          } catch (InterruptedException | ExecutionException ex) {
            String title = "Unable to export to snap file";
            String message = "Unexpected error";
            LOGGER.log(Level.SEVERE, title, ex);

            Throwable cause = ex.getCause();
            /*if (cause != null && cause instanceof CommandException) {
                message = cause.getMessage();
            }*/

            JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE);
          } finally {
            frame.hideModalWait();
          }
        }
      }.execute();
    } else {
      // System.out.println("File access cancelled by user.");
    }
  }
}
