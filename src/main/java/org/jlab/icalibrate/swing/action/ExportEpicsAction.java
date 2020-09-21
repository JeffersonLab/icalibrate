package org.jlab.icalibrate.swing.action;

import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import org.jlab.icalibrate.epics.ChannelManager;
import org.jlab.icalibrate.exception.AppException;
import org.jlab.icalibrate.model.DoseRateTripSetpoint;
import org.jlab.icalibrate.swing.ICalibrateFrame;
import org.jlab.icalibrate.swing.worker.MinimumExecutionSwingWorker;

/**
 * Handle an export EPICS values request.  
 * 
 * This class is an Action and not simply an ActionListener because the label (name) is used to
 * set the ChooseAndModifySetpointDialog button label.
 * 
 * @author ryans
 */
public final class ExportEpicsAction extends AbstractAction {

    private static final Logger LOGGER = Logger.getLogger(ExportSnapAction.class.getName());

    private final ICalibrateFrame frame;

    /**
     *
     * @param frame The ICalibrateFrame
     */
    public ExportEpicsAction(ICalibrateFrame frame) {
        this.frame = frame;
        putValue(AbstractAction.NAME, "Write to EPICS");
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        frame.getModifySetpointDialog().setVisible(false);

        if (true) {
            frame.queueShowModalWait();
            new MinimumExecutionSwingWorker<Void, Void>() {

                @Override
                protected Void doWithMinimumExecution() throws Exception {

                    ChannelManager manager = frame.getChannelManager();
                    List<DoseRateTripSetpoint> setpointList
                            = frame.getModifySetpointDialog().getSetpoints();

                    for (DoseRateTripSetpoint setpoint : setpointList) {
                        String pv = setpoint.getIonChamber().getDoseRateSetpointWritePvName();
                        double value = setpoint.getDoseRateRadsPerHour();

                        try {
                            manager.put(pv, value);
                        } catch (CAException e) {
                            throw new AppException("Unable to perform EPICS CA Put of PV: " + pv
                                    + " - " + e.getMessage(), e);
                        } catch (TimeoutException e) {
                            throw new AppException("Timeout waiting for EPICS CA Put of PV: " + pv,
                                    e);
                        }
                    }

                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get(); // See if there were any exceptions
                    } catch (InterruptedException | ExecutionException ex) {
                        String title = "Unable to export to EPICS";
                        String message = "Unexpected error";
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
                        JOptionPane.showMessageDialog(frame, message, title,
                                JOptionPane.ERROR_MESSAGE);
                    } finally {
                        frame.hideModalWait();
                    }
                }
            }.execute();
        }
    }
}
