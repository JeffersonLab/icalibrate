package org.jlab.icalibrate.swing.action.listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jlab.elog.Body;
import org.jlab.elog.LogEntry;
import org.jlab.icalibrate.ICalibrateApp;
import org.jlab.icalibrate.IOUtil;
import org.jlab.icalibrate.model.ChartDataset;
import org.jlab.icalibrate.model.DoseRateMeasurement;
import org.jlab.icalibrate.model.HallCalibrationDataset;
import org.jlab.icalibrate.swing.ICalibrateFrame;
import org.jlab.icalibrate.swing.util.DoseRateChartPanel;
import org.jlab.icalibrate.swing.util.HyperLinkEnabledMessage;
import org.jlab.icalibrate.swing.worker.MinimumExecutionSwingWorker;

/**
 * Handle an export to elog request.
 *
 * @author ryans
 */
public final class ExportElogActionListener implements ActionListener {

    private static final Logger LOGGER = Logger.getLogger(ExportElogActionListener.class.getName());

    private final ICalibrateFrame frame;

    /**
     * Create a new OpenHCDActionListener.
     *
     * @param frame The ICalibrateFrame
     */
    public ExportElogActionListener(ICalibrateFrame frame) {
        this.frame = frame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        frame.queueShowModalWait();
        new MinimumExecutionSwingWorker<Long, Void>() {

            @Override
            protected Long doWithMinimumExecution() throws Exception {
                Long lognumber = null;

                String books = ICalibrateApp.APP_PROPERTIES.getProperty(
                        "LOGBOOK_CSV");

                HallCalibrationDataset ds = frame.getDataset();

                LogEntry entry = new LogEntry("iCalibrate: Hall " + ds.getHall().name(), books);

                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MMM-dd HH:mm");
                DecimalFormat doseRateFormatter = new DecimalFormat("###,##0");
                DecimalFormat currentFormatter = new DecimalFormat("###,##0.00");

                String body = "<h3 style=\"color: gold; background-color: purple;\">Calibration Parameters</h3>"
                        + "<table>"
                        + "<tbody>"
                        + "<tr><th>Hall:</th><td>" + IOUtil.escapeXml(ds.getHall().name()) + "</td></tr>"
                        + "<tr><th>Target:</th><td>" + IOUtil.escapeXml(ds.getTarget()) + "</td></tr>"
                        + "<tr><th>Pass:</th><td>" + IOUtil.escapeXml(ds.getPass()) + "</td></tr>"
                        + "<tr><th>Note:</th><td>" + IOUtil.escapeXml(ds.getNote()) + "</td></tr>"
                        + "<tr><th>Calibrated On:</th><td>" + IOUtil.escapeXml(formatter.format(ds.getCalibratedDate())) + "</td></tr>"
                        + "<tr><th>Calibrated By:</th><td>" + IOUtil.escapeXml(ds.getCalibratedBy()) + "</td></tr>"
                        + "<tr><th>File:</th><td>" + IOUtil.escapeXml(frame.getFilename()) + "</td></tr>"
                        + "<tr><th>Setpoint Current:</th><td>" + currentFormatter.format(frame.getCurrent()) + " " + IOUtil.escapeXml(frame.getCurrentUnits()) + "</td></tr>"
                        + "<tr><th>Setpoint Margin:</th><td>" + frame.getSignedMargin() + "%</td></tr>"
                        + "</tbody>"
                        + "</table>";

                List<File> tmpList = new ArrayList<>();

                try {
                    String currentUnits = frame.getCurrentUnits();
                    DoseRateChartPanel tmpPanel = new DoseRateChartPanel();
                    File tmp;

                    int figureCount = 1;

                    for (ChartDataset dataset : frame.getChartDatasetList()) {
                        tmpPanel.setDataset(dataset, currentUnits);
                        JFreeChart chart = tmpPanel.getChart();
                        String caption = chart.getTitle().getText();
                        tmp = File.createTempFile(caption, ".png");
                        tmpList.add(tmp);
                        FileOutputStream out = new FileOutputStream(tmp);
                        ChartUtilities.writeChartAsPNG(out, chart, 400, 300);
                        entry.addAttachment(tmp.getCanonicalPath(), "");

                        Double controlSystemSetpoint = frame.getControlSystemSetpoint(figureCount - 1);

                        if (controlSystemSetpoint == null) {
                            controlSystemSetpoint = 0d;
                        }

                        body = body + "<h3 style=\"color: gold; background-color: purple;\">" + IOUtil.escapeXml(caption) + "</h3>"
                                + "<table>"
                                + "<tbody>"
                                + "<tr><th>Calculated Setpoint:</th><td>" + IOUtil.escapeXml(doseRateFormatter.format(dataset.getSetpoint())) + " rads/hr</td></tr>"
                                + "<tr><th>Actual Setpoint:</th><td>" + IOUtil.escapeXml(doseRateFormatter.format(controlSystemSetpoint)) + " rads/hr</td></tr>"
                                + "<tr><th>Fit Equation:</th><td>" + IOUtil.escapeXml(dataset.getFitEquation()) + "</td></tr>"
                                + "<tr><th>R<sup>2</sup>:</th><td>" + IOUtil.escapeXml(dataset.getRSquareLabel()) + "</td></tr>"
                                + "</tbody>"
                                + "</table>"
                                + "[figure:" + figureCount++ + "]"
                                + "<table>"
                                + "<thead>"
                                + "<tr><th>Current (" + IOUtil.escapeXml(frame.getCurrentUnits()) + ")</th><th>Dose Rate (rads/hr)</th></tr>"
                                + "</thead>"
                                + "<tbody>";

                        List<DoseRateMeasurement> measurementList = dataset.getMeasuredDataset().getMeasurementList();
                        for (DoseRateMeasurement measurement : measurementList) {
                            body = body + "<tr><td>" + currentFormatter.format(measurement.getCurrent()) + "</td><td>" + doseRateFormatter.format(measurement.getDoseRateRadsPerHour()) + "</td></tr>";
                        }

                        body = body + "</tbody>"
                                + "</table>";
                    }

                    entry.setBody(body, Body.ContentType.HTML);

                    lognumber = entry.submitNow();
                } finally {
                    for (File tmp : tmpList) {
                        if (tmp != null) {
                            tmp.delete();
                        }
                    }
                }

                return lognumber;
            }

            @Override
            protected void done() {
                try {
                    Long lognumber = get(); // See if there were any exceptions

                    String url = "https://logbooks.jlab.org/entry/" + lognumber;
                    String html = "<html>Log number: <a href=\"" + url + "\">" + lognumber + "</a></html>";

                    JOptionPane.showMessageDialog(frame, new HyperLinkEnabledMessage(html), "Successfully created elog", JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException | ExecutionException ex) {
                    String title = "Unable to create elog";
                    String message = "Unexpected error";
                    LOGGER.log(Level.SEVERE, title, ex);

                    Throwable cause = ex.getCause();
                    if (cause != null) {
                        message = ex.getMessage();
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
