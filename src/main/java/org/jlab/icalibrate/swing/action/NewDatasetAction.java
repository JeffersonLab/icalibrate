package org.jlab.icalibrate.swing.action;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.jlab.icalibrate.model.CreateNewDatasetParameters;
import org.jlab.icalibrate.swing.ICalibrateFrame;
import org.jlab.icalibrate.swing.dialog.CreateDatasetProgressDialog;
import org.jlab.icalibrate.swing.worker.CreateNewDatasetWorker;

/**
 * Handle a create new dataset request.
 * 
 * This class is an Action and not simply an ActionListener because the label (name) is used to
 * replace the default Wizard finish button label.
 *
 * @author ryans
 */
public final class NewDatasetAction extends AbstractAction {

    private static final Logger LOGGER = Logger.getLogger(NewDatasetAction.class.getName());

    /**
     * The frame.
     */
    private final ICalibrateFrame frame;

    /**
     * The params.
     */
    private final CreateNewDatasetParameters params;

    /**
     * The progress dialog.
     */
    private final CreateDatasetProgressDialog progressDialog;

    /**
     * Create a new NewDatasetAction.
     *
     * @param frame The ICalibrateFrame
     * @param progressDialog The progress dialog
     * @param params The parameters
     */
    public NewDatasetAction(ICalibrateFrame frame, CreateDatasetProgressDialog progressDialog,
            CreateNewDatasetParameters params) {
        this.frame = frame;
        this.params = params;
        this.progressDialog = progressDialog;
        putValue(AbstractAction.NAME, "Start");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        
        frame.closeHallCalibrationDataset();
        
        progressDialog.updateProgressPercent(0);

        CreateNewDatasetWorker worker = new CreateNewDatasetWorker(params, progressDialog, frame);

        worker.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("progress".equals(evt.getPropertyName())) {
                    progressDialog.updateProgressPercent((Integer) evt.getNewValue());
                } else if ("incremental".equals(evt.getPropertyName())) {
                    progressDialog.updateProgressText((CreateNewDatasetWorker.IncrementalResultChunk) evt.getNewValue());
                }
            }
        });

        progressDialog.setParamsAndCancelTarget(params, worker);

        worker.execute();

        progressDialog.pack();
        progressDialog.setLocationRelativeTo(frame);
        progressDialog.setVisible(true);
    }
}
