package org.jlab.icalibrate.swing.action;

import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.jlab.jlog.Body;
import org.jlab.jlog.LogEntry;
import org.jlab.jlog.exception.LogCertificateException;
import org.jlab.jlog.exception.LogIOException;
import org.jlab.icalibrate.ICalibrateApp;
import org.jlab.icalibrate.epics.ChannelManager;
import org.jlab.icalibrate.exception.AppException;
import org.jlab.icalibrate.IOUtil;
import org.jlab.icalibrate.model.DoseRateTripSetpoint;
import org.jlab.icalibrate.model.HallCalibrationDataset;
import org.jlab.icalibrate.swing.ICalibrateFrame;
import org.jlab.icalibrate.swing.dialog.ChooseAndModifySetpointDialog;
import org.jlab.icalibrate.swing.table.model.ModifySetpointTableModel;
import org.jlab.icalibrate.swing.util.HyperLinkEnabledMessage;
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

    /**
     * The frame.
     */
    private final ICalibrateFrame frame;

    /**
     * Create a new ExportEpicsAction.
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
            new MinimumExecutionSwingWorker<Long, Void>() {

                @Override
                protected Long doWithMinimumExecution() throws Exception {

                    ChannelManager manager = frame.getChannelManager();
                    List<DoseRateTripSetpoint> setpointList
                            = frame.getModifySetpointDialog().getSetpoints();

                    boolean writeAllowed = "true".equals(ICalibrateApp.APP_PROPERTIES.getProperty(
                            "WRITE_ALLOWED"));

                    if (writeAllowed) {
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
                    }

                    Long lognumber = null;

                    try {
                        lognumber = sendComparisonElog();
                    } catch(LogCertificateException | LogIOException e) {
                        LOGGER.log(Level.WARNING, "Unable to create export to EPICS elog comparision", e);
                    }

                    return lognumber;
                }

                @Override
                protected void done() {
                    try {
                        Long lognumber = get(); // See if there were any exceptions

                        if(lognumber != null) { // failing to create comparison elog is simply ignored
                            String url = "https://logbooks.jlab.org/entry/" + lognumber;
                            String html = "<html>Log number: <a href=\"" + url + "\">" + lognumber + "</a></html>";

                            JOptionPane.showMessageDialog(frame, new HyperLinkEnabledMessage(html), "Successfully created elog", JOptionPane.INFORMATION_MESSAGE);
                        }
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

    private Long sendComparisonElog() throws LogCertificateException, LogIOException {
        Long lognumber = null;

        String books = ICalibrateApp.APP_PROPERTIES.getProperty(
                "LOGBOOK_CSV");

        HallCalibrationDataset ds = frame.getDataset();

        LinkedHashSet<ModifySetpointTableModel.ModifySetpointRow> rows = frame.getModifySetpointDialog().getData();

        LogEntry entry = new LogEntry("iCalibrate: Hall " + ds.getHall().name(), books);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MMM-dd HH:mm");
        DecimalFormat doseRateFormatter = new DecimalFormat("###,##0");
        DecimalFormat currentFormatter = new DecimalFormat("###,##0.00");

        String body = "<h3 style=\"font-weight: bold;\">Calibration Parameters</h3>"
                + "<table>"
                + "<tbody>"
                + "<tr><th>Hall:</th><td>" + IOUtil.escapeXml(ds.getHall().name()) + "</td></tr>"
                + "<tr><th>Target:</th><td>" + IOUtil.escapeXml(ds.getTarget()) + "</td></tr>"
                + "<tr><th>Pass:</th><td>" + IOUtil.escapeXml(ds.getPass()) + "</td></tr>"
                + "<tr><th>Note:</th><td>" + IOUtil.escapeXml(ds.getNote()) + "</td></tr>"
                + "<tr><th>Calibrated On:</th><td>" + IOUtil.escapeXml(formatter.format(ds.getCalibratedDate())) + "</td></tr>"
                + "<tr><th>Calibrated By:</th><td>" + IOUtil.escapeXml(ds.getCalibratedBy()) + "</td></tr>"
                + "<tr><th>File:</th><td>" + IOUtil.escapeXml(frame.getFilename()) + "</td></tr>"
                + "<tr><th></th><td></td></tr>"
                + "</tbody>"
                + "</table>"
                + "<hr/>"
                + "<table>"
                + "<thead>"
                + "<tr><th>Ion Chamber</th><th>Existing Setpoing (rads/hr)</th><th>New Setpoint (rads/hr)</th></tr>"
                + "</thead>"
                + "<tbody>";

        for(ModifySetpointTableModel.ModifySetpointRow row: rows) {

            String ic = IOUtil.escapeXml(row.getIonChamber().getFriendlyNameOrEpicsName());
            String existing = "";
            String replacement = "";

            if(row.getExisting() != null) {
                existing = doseRateFormatter.format(row.getExisting().doubleValue());
            }

            if(row.getNewValue() != null) {
                replacement = doseRateFormatter.format(row.getNewValue().doubleValue());
            }

            if(Boolean.TRUE.equals(row.getIncluded())) {
                body = body + "<tr><th>" + ic + "</th><td>" + existing + "</td><td>" + replacement + "</td></tr>";
            }
        }

        body = body + "</tbody>"
                + "</table>"
                + "<hr/>";


        entry.setBody(body, Body.ContentType.HTML);

        lognumber = entry.submitNow();

        return lognumber;
    }
}
