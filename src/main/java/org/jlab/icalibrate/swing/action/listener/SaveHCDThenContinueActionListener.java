package org.jlab.icalibrate.swing.action.listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jlab.icalibrate.ICalibrateApp;
import org.jlab.icalibrate.file.io.DatasetFileWriter;
import org.jlab.icalibrate.swing.generated.ICalibrateFrame;
import org.jlab.icalibrate.swing.chooser.ConfirmOverwriteFileChooser;

/**
 * Handle an action request that first requires saving a Hall Calibration Data file before
 * continuing with another action. The continue action can be null in the event that no action is to
 * be performed afterwards. Generally a PromptUnsavedThenContinueActionListener is used to decide if
 * this action (save action) should be performed before closing the application.
 *
 * @author ryans
 */
public final class SaveHCDThenContinueActionListener implements ActionListener {

    private static final Logger LOGGER = Logger.getLogger(
            SaveHCDThenContinueActionListener.class.getName());

    private final ICalibrateFrame frame;
    private final ActionListener continueAction;

    /**
     * Create a new SaveHCDThenContinueActionListener.
     *
     * @param frame The parent frame of the wait dialog
     * @param continueAction The continue action, or null if none
     */
    public SaveHCDThenContinueActionListener(ICalibrateFrame frame, ActionListener continueAction) {
        this.frame = frame;
        this.continueAction = continueAction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean cancelled = false;

        String fn = frame.getFilename();
        String dir = ICalibrateApp.APP_PROPERTIES.getProperty("DEFAULT_HCD_FILE_DIR");

        if (dir != null) {
            fn = dir + File.separator + fn;
        }

        ConfirmOverwriteFileChooser saveDatasetFileChooser
                = new ConfirmOverwriteFileChooser();

        saveDatasetFileChooser.setDialogTitle("Choose Hall Calibration Dataset File");
        saveDatasetFileChooser.setSelectedFile(new File("icalibrate.hcd"));
        saveDatasetFileChooser.setFileFilter(new FileNameExtensionFilter(
                "Hall Calibration Dataset (*.hcd)",
                "hcd"));

        saveDatasetFileChooser.setSelectedFile(new File(fn));
        int returnVal = saveDatasetFileChooser.showSaveDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = saveDatasetFileChooser.getSelectedFile();
            try {
                DatasetFileWriter writer = new DatasetFileWriter();

                writer.write(file, frame.getDataset());

                frame.setTitle(file.getName() + " - iCalibrate");
                frame.setStateSaved(true);
                doContinueAction();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Unable to save file",
                        JOptionPane.ERROR_MESSAGE);
                System.err.println("Problem accessing file: " + file.getAbsolutePath());
                ex.printStackTrace();
            }
        } else {
            //System.out.println("File access cancelled by user.");
            cancelled = true;
        }
    }

    private void doContinueAction() {
        if (continueAction != null) {
            continueAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                    "Continue"));
        }
    }
}
