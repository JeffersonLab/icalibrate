package org.jlab.icalibrate.swing.worker;

import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.jlab.icalibrate.ICalibrateApp;
import org.jlab.icalibrate.epics.ChannelManager;
import org.jlab.icalibrate.epics.PvListener;
import org.jlab.icalibrate.exception.AppException;
import org.jlab.icalibrate.exception.FSDException;
import org.jlab.icalibrate.model.DoseRateMeasurement;
import org.jlab.icalibrate.model.Hall;
import org.jlab.icalibrate.model.HallCalibrationDataset;
import org.jlab.icalibrate.model.IonChamber;
import org.jlab.icalibrate.model.IonChamberDataset;
import org.jlab.icalibrate.model.CreateNewDatasetParameters;
import org.jlab.icalibrate.swing.ICalibrateFrame;
import org.jlab.icalibrate.swing.dialog.CreateDatasetProgressDialog;
import org.jlab.icalibrate.swing.worker.CreateNewDatasetWorker.IncrementalResultChunk;

/**
 * This is a SwingWorker which performs the data gathering work for ion chamber
 * calibration. The process includes ramping the attenuator in order to
 * incrementally step current and measure dose rate changes. At a given current
 * multiple samples are taken and averaged to obtain a more accurate measure.
 *
 * @author ryans
 */
public class CreateNewDatasetWorker extends SwingWorker<List<IonChamberDataset>, IncrementalResultChunk> implements
        PvListener {

    private final Object fsdLock = new Object();
    private final Object fsdConnectLock = new Object();

    private static final Logger LOGGER = Logger.getLogger(CreateNewDatasetWorker.class.getName());

    public static final long SAMPLE_FREQUENCY_MILLIS = 1000;

    private final CreateDatasetProgressDialog progressDialog;
    private final ICalibrateFrame frame;
    private final CreateNewDatasetParameters params;

    private volatile boolean fsdTripThisInstant = false;
    private volatile boolean fsdTripDuringStep = false;

    /**
     * If user aborts (via cancel button) worker thread 
     * (thread interrupt) then CancellationException is
     * thrown and partial results are unavailable.  We stash the partial results
     * here so we can try to salvage whatever we have.
     */
    private volatile List<IonChamberDataset> partialDatasetList;
    
    /**
     * Create a new CreateNewDatasetWorker.
     *
     * @param params The process parameters
     * @param progressDialog The progress dialog
     * @param frame The parent frame
     */
    public CreateNewDatasetWorker(CreateNewDatasetParameters params,
            CreateDatasetProgressDialog progressDialog,
            ICalibrateFrame frame) {
        this.params = params;
        this.progressDialog = progressDialog;
        this.frame = frame;

        LOGGER.log(Level.FINEST, "Number of Steps: {0}", params.getNumberOfSteps());
        LOGGER.log(Level.FINEST, "Number of Samples: {0}", params.getSamplesPerStep());
        LOGGER.log(Level.FINEST, "Attenuator Range: {0}", params.computeAttenuatorRange());
        LOGGER.log(Level.FINEST, "Attenuator Step Magnitude: {0}", params.computeStepSize());
    }

    public synchronized void setFsdTripThisInstant(boolean fsdTripThisInstant) {
        this.fsdTripThisInstant = fsdTripThisInstant;
    }

    public synchronized boolean isFsdTripThisInstant() {
        return fsdTripThisInstant;
    }

    public synchronized void setFsdTripDuringStep(boolean fsdTripDuringStep) {
        this.fsdTripDuringStep = fsdTripDuringStep;
    }

    public synchronized boolean isFsdTripDuringStep() {
        return fsdTripDuringStep;
    }

    @Override
    protected List<IonChamberDataset> doInBackground() throws Exception {
        try {
            synchronized (fsdConnectLock) {
                frame.getChannelManager().addPv(this, loadMasterFsdPv());
                fsdConnectLock.wait();
            }

            List<IonChamber> icList = params.getIonChamberList();
            HallPvSet pvs = loadPvs();

            List<IonChamberDataset> datasetList = new ArrayList<>();
            List<List<DoseRateMeasurement>> measurementMatrix = new ArrayList<>();

            // Populate matrix with empty rows
            icList.stream().forEach((item) -> {
                measurementMatrix.add(new ArrayList<>());
            });

            // Determine attenuator setting so we can restore it back where we found it later //
            ChannelManager manager = frame.getChannelManager();
            DBR dbr;
            double originalAttenuator;

            try {
                dbr = manager.get(pvs.hallAttenuatorPv);
            } catch (CAException e) {
                throw new AppException("Unable to perform EPICS CA Get of PV: "
                        + pvs.hallAttenuatorPv
                        + " - " + e.getMessage(), e);
            } catch (TimeoutException e) {
                throw new AppException("Timeout waiting for EPICS CA Get of PV: "
                        + pvs.hallAttenuatorPv,
                        e);
            }

            if (dbr != null && dbr.isDOUBLE()) {
                originalAttenuator = ((gov.aps.jca.dbr.DOUBLE) dbr).getDoubleValue()[0];
            } else {
                throw new AppException("Hall attenuator record is null or not a double");
            }

            // Start stepping over attenuator increases
            int attenuator = params.getMinAttenuator();

            LOGGER.log(Level.FINEST, "Initial attenuator value: {0}", attenuator);

            try {
                doStepLoop(attenuator, icList, measurementMatrix, pvs);
            } catch(InterruptedException e) {
                LOGGER.log(Level.FINEST, "Background worker thread canceled / interrupted");
                // Instead of allowing exception to bubble all the way up we catch it
                // here so we can continue here and attempt to keep whatever data
                // we do have and to clean up gracefully.
            }
            
            doCleanUp(manager, pvs, originalAttenuator);

            //// PUT DATA INTO DATASET ////
            for (int i = 0; i < icList.size(); i++) {
                IonChamber ic = icList.get(i);
                List<DoseRateMeasurement> measurementList = measurementMatrix.get(i);
                IonChamberDataset dataset = new IonChamberDataset(ic, measurementList);
                datasetList.add(dataset);
            }

            // In case we've been cancelled then calling get() will result in 
            // CancellationException so 
            // we stash results here so we can salvage them
            partialDatasetList = datasetList;
            
            return datasetList;
        } finally {
            frame.getChannelManager().removeListener(this);
        }
    }

    private void doCleanUp(ChannelManager manager, HallPvSet pvs, double originalAttenuator) throws AppException {
            // Put attenuator back where we found it
            boolean writeAllowed = "true".equals(ICalibrateApp.APP_PROPERTIES.getProperty(
                    "WRITE_ALLOWED"));

            //System.out.println("CAPUT attenuator value: " + attenuator);
            if (writeAllowed) {
                try {
                    manager.put(pvs.hallAttenuatorPv, originalAttenuator);
                } catch (CAException e) {
                    throw new AppException("Unable to perform EPICS CA Put of PV: "
                            + pvs.hallAttenuatorPv
                            + " - " + e.getMessage(), e);
                } catch (TimeoutException e) {
                    throw new AppException("Timeout waiting for EPICS CA Put of PV: "
                            + pvs.hallAttenuatorPv,
                            e);
                }

                try {
                    manager.put(pvs.hallModePv, 0); // 0 = BEAM_SYNC
                } catch (CAException e) {
                    throw new AppException("Unable to perform EPICS CA Put of PV: "
                            + pvs.hallModePv
                            + " - " + e.getMessage(), e);
                } catch (TimeoutException e) {
                    throw new AppException("Timeout waiting for EPICS CA Put of PV: "
                            + pvs.hallModePv,
                            e);
                }
            } else {
                LOGGER.log(Level.WARNING, "Not writing due to configuration");
            }        
    }
    
    private void doStepLoop(int attenuator, List<IonChamber> icList, List<List<DoseRateMeasurement>> measurementMatrix, HallPvSet pvs) throws InterruptedException, AppException {
            for (int i = 0; i <= params.getNumberOfSteps(); i++) {
                boolean fsdExceptionEncountered;

                do {
                    fsdExceptionEncountered = false;
                    setFsdTripDuringStep(false);               
                    try {
                        doStep(i, attenuator, icList, measurementMatrix, pvs);
                    } catch (FSDException e) {
                        LOGGER.log(Level.FINEST, "FSD Encountered");
                        fsdExceptionEncountered = true;
                        synchronized (fsdLock) { // Wait for user to decide to resume or not
                            publish(new IncrementalResultChunk(WorkerState.WAITING_ON_FSD, i, null));
                            fsdLock.wait();
                        }
                        LOGGER.log(Level.FINEST, "User must have chose resume, because here we are");
                    }
                } while (fsdExceptionEncountered);

                attenuator = attenuator + params.computeStepSize();

                if (i == params.getNumberOfSteps()) { // if last step may have to jump to end value
                    attenuator = params.getMaxAttenuator();
                } else if (attenuator > params.getMaxAttenuator()) { // for safety, should not happen
                    attenuator = params.getMaxAttenuator();
                }

                LOGGER.log(Level.FINEST, "Attenuator value now: {0}", attenuator);

                int progress = i * (100 / params.getNumberOfSteps());
                //System.out.println("setting progress: " + progress);
                this.setProgress(progress); // Set percent progress for property change listeners   
            }        
    }
    
    private void doStep(int step, int attenuator, List<IonChamber> icList,
            List<List<DoseRateMeasurement>> measurementMatrix, HallPvSet pvs) throws
            InterruptedException,
            AppException {

        if (this.isFsdTripThisInstant()) {
            throw new FSDException("No sense in starting this step without first clearing the FSD");
        }

        DBR dbr;
        ChannelManager manager = frame.getChannelManager();

        LOGGER.log(Level.FINEST, "Step: {0}", step);
        publish(new IncrementalResultChunk(WorkerState.ADJUSTING, step, null));

        boolean writeAllowed = "true".equals(ICalibrateApp.APP_PROPERTIES.getProperty(
                "WRITE_ALLOWED"));

        //System.out.println("CAPUT attenuator value: " + attenuator);
        if (writeAllowed) {
            try {
                manager.put(pvs.hallAttenuatorPv, attenuator);
            } catch (CAException e) {
                throw new AppException("Unable to perform EPICS CA Put of PV: "
                        + pvs.hallAttenuatorPv
                        + " - " + e.getMessage(), e);
            } catch (TimeoutException e) {
                throw new AppException("Timeout waiting for EPICS CA Put of PV: "
                        + pvs.hallAttenuatorPv,
                        e);
            }
        } else {
            LOGGER.log(Level.WARNING, "Not writing due to configuration");
        }

        publish(new IncrementalResultChunk(WorkerState.SETTLING, step, null));

        Thread.sleep(params.getSettleSeconds() * 1000);

        double current = 0.0d;

        publish(new IncrementalResultChunk(WorkerState.READING, step, null));

        try {
            dbr = manager.get(pvs.hallBeamCurrentReadbackPv);
        } catch (CAException e) {
            throw new AppException("Unable to perform EPICS CA Get of PV: "
                    + pvs.hallBeamCurrentReadbackPv
                    + " - " + e.getMessage(), e);
        } catch (TimeoutException e) {
            throw new AppException("Timeout waiting for EPICS CA Get of PV: "
                    + pvs.hallBeamCurrentReadbackPv,
                    e);
        }

        if (dbr != null && dbr.isDOUBLE()) {
            current = ((gov.aps.jca.dbr.DOUBLE) dbr).getDoubleValue()[0];
        } else {
            LOGGER.log(Level.WARNING, "Current is null or not a double");
        }

        //LOGGER.log(Level.FINEST, "Current Reading: {0}", current);
        measure(icList, measurementMatrix, manager, current, step);       
    }

    private void measure(List<IonChamber> icList, List<List<DoseRateMeasurement>> measurementMatrix,
            ChannelManager manager, double current, int step) throws AppException,
            InterruptedException {
        List<List<Double>> sampleMatrix = new ArrayList<>();

        // Populate matrix with empty rows
        icList.stream().forEach((item) -> {
            sampleMatrix.add(new ArrayList<>());
        });

        sample(icList, sampleMatrix, manager, step);

        // Note: once we've made it past sampling so we can calculate averages and add results to measurementLists
        // If FSD Trip happened, during sampling we don't get here as exception bubbles up
        for (int j = 0; j < icList.size(); j++) {
            IonChamber ic = icList.get(j);
            List<Double> sampleList = sampleMatrix.get(j);

            /*for(int k = 0; k < datasetList.size(); k++) {
                LOGGER.log(Level.FINEST, "Sample {0} = {1}", new Object[]{k, datasetList.get(k)});
            }*/
            // Calculate the average of the samples
            double doseRate = sampleList.stream().mapToDouble(a -> a).average().orElse(Double.NaN);

            //LOGGER.log(Level.FINEST, "Average dose rate: {0}", doseRate);
            DoseRateMeasurement measurement = new DoseRateMeasurement(current, doseRate);
            List<DoseRateMeasurement> measurementList = measurementMatrix.get(j);
            measurementList.add(measurement);
        }
    }

    private void sample(List<IonChamber> icList, List<List<Double>> sampleMatrix,
            ChannelManager manager, int step) throws AppException, InterruptedException {
        DBR dbr;

        for (int i = 0; i < params.getSamplesPerStep(); i++) {

            LOGGER.log(Level.FINEST, "Sample: {0}", i + 1);

            publish(new IncrementalResultChunk(WorkerState.SAMPLING, step, i + 1));

            for (int j = 0; j < icList.size(); j++) {
                IonChamber ic = icList.get(j);
                double doseRate = 0.0;

                //LOGGER.log(Level.FINEST, "Dose Read PV: -{0}-", ic.getDoseRateReadPvName());
                try {
                    dbr = manager.get(ic.getDoseRateReadPvName());
                    //dbr = manager.get(ic.getDoseRateSetpointReadPvName());
                } catch (CAException e) {
                    throw new AppException("Unable to perform EPICS CA Get of PV: "
                            + ic.getDoseRateReadPvName()
                            + " - " + e.getMessage(), e);
                } catch (TimeoutException e) {
                    throw new AppException("Timeout waiting for EPICS CA Get of PV: "
                            + ic.getDoseRateReadPvName(),
                            e);
                }

                if (dbr != null && dbr.isDOUBLE()) {
                    doseRate = ((gov.aps.jca.dbr.DOUBLE) dbr).getDoubleValue()[0];
                } else if (dbr != null && dbr.isINT()) {
                    doseRate = ((gov.aps.jca.dbr.INT) dbr).getIntValue()[0];
                } else {
                    LOGGER.log(Level.WARNING, "doseRate is null or not a double");
                }

                List<Double> sampleList = sampleMatrix.get(j);
                sampleList.add(doseRate);
                //LOGGER.log(Level.FINEST, "IC {0}, Dose reading: {1}", new Object[]{ic.getFriendlyName(), doseRate});
            }

            if (i < (params.getSamplesPerStep() - 1)) {
                publish(new IncrementalResultChunk(WorkerState.DWELLING, step, i + 1));

                Thread.sleep(SAMPLE_FREQUENCY_MILLIS);
            } else {
                //LOGGER.log(Level.FINEST, "No need to dwell on last dataset");
            }

            if (this.isFsdTripDuringStep()) {
                throw new FSDException("FSD Trip while sampling");
            }
        }
    }

    @Override
    protected void done() {
        progressDialog.setVisible(false);

        Date calibratedDate = new Date();
        String calibratedBy = System.getProperty("user.name");

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'_'HHmm");

        String target = params.getTarget();

        if (target == null) {
            target = "Unknown";
        }

        target = target.replaceAll("[^a-zA-Z0-9\\.\\-]", "_"); // Replace anything other than alpha numeric characters plus period and underscore (whitespace, slashes, etc) with underscore
        
        String pass = params.getPass();

        if (pass == null) {
            pass = "Unknown";
        }

        pass = pass.replaceAll("\\s+", "_"); // Replace whitespace with underscore

        String filename = params.getHall().name() + "-" + formatter.format(calibratedDate) + "_"
                + target + "_Pass-" + pass + ".hcd";
        List<IonChamberDataset> datasetList;

        try {
            datasetList = get();

            frame.setDataset(new HallCalibrationDataset(params.getHall(), params.getTarget(),
                    params.getPass(), params.getNote(), calibratedDate, calibratedBy,
                    datasetList),
                    filename, false);
        } catch (CancellationException e) {
            LOGGER.log(Level.FINEST, "New dataset canceled, attempt to salvage partial results");
            
            if(partialDatasetList != null && 
                    partialDatasetList.size() > 0 && 
                    partialDatasetList.get(0) != null && 
                    partialDatasetList.get(0).getMeasurementList() != null &&
                    partialDatasetList.get(0).getMeasurementList().size() > 0) {
                frame.setDataset(new HallCalibrationDataset(params.getHall(), params.getTarget(),
                    params.getPass(), params.getNote(), calibratedDate, calibratedBy,
                    partialDatasetList),
                    filename, false);
            } else {
                LOGGER.log(Level.FINEST, "Empty dataset ignored");
            }
        } catch (ExecutionException | InterruptedException | RuntimeException ex) {
            String title = "Unable to create new dataset";
            String message = ex.getMessage();

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

            LOGGER.log(Level.WARNING, title, ex);

            JOptionPane.showMessageDialog(frame, message, title,
                    JOptionPane.ERROR_MESSAGE);
            frame.closeHallCalibrationDataset(); // Clean up state otherwise might prompt to save bogus state
        }
    }

    @Override
    public void process(List<IncrementalResultChunk> chunkList) {
        firePropertyChange("incremental", null, chunkList.get(chunkList.size() - 1));
    }

    private HallPvSet loadPvs() {
        HallPvSet pvs = new HallPvSet();

        String hallCurrentProperty = "HALLA_CURRENT_READ_PV";
        String hallAttenuatorProperty;
        String hallModeProperty;

        if (params.getHall() == Hall.C) {
            hallCurrentProperty = "HALLC_CURRENT_READ_PV";
        } else if (params.getHall() == Hall.D) {
            hallCurrentProperty = "HALLD_CURRENT_READ_PV";
        }

        switch (params.getLaser()) {
            case A:
                hallAttenuatorProperty = "LASERA_ATTENUATOR_PV";
                hallModeProperty = "LASERA_MODE_PV";
                break;
            case B:
                hallAttenuatorProperty = "LASERB_ATTENUATOR_PV";
                hallModeProperty = "LASERB_MODE_PV";
                break;
            case C:
                hallAttenuatorProperty = "LASERC_ATTENUATOR_PV";
                hallModeProperty = "LASERC_MODE_PV";
                break;
            case D:
                hallAttenuatorProperty = "LASERD_ATTENUATOR_PV";
                hallModeProperty = "LASERD_MODE_PV";
                break;
            default:
                throw new RuntimeException("Unknown laser");
        }

        pvs.hallBeamCurrentReadbackPv = ICalibrateApp.APP_PROPERTIES.getProperty(
                hallCurrentProperty);
        pvs.hallAttenuatorPv = ICalibrateApp.APP_PROPERTIES.getProperty(hallAttenuatorProperty);
        pvs.hallModePv = ICalibrateApp.APP_PROPERTIES.getProperty(hallModeProperty);
        
        if (pvs.hallBeamCurrentReadbackPv != null) {
            pvs.hallBeamCurrentReadbackPv = pvs.hallBeamCurrentReadbackPv.trim(); // Config file might have spaces!
        }

        if (pvs.hallAttenuatorPv != null) {
            pvs.hallAttenuatorPv = pvs.hallAttenuatorPv.trim(); // Config file might have spaces!
        }

        if (pvs.hallModePv != null) {
            pvs.hallModePv = pvs.hallModePv.trim(); // Config file might have spaces!
        }        
        
        return pvs;
    }

    private String loadMasterFsdPv() {
        String fsdPv = ICalibrateApp.APP_PROPERTIES.getProperty("MASTER_FSD_VOLTAGE_PV");

        if (fsdPv != null) {
            fsdPv = fsdPv.trim(); // Config file might have spaces!
        }

        return fsdPv;
    }

    /*private void clearLastMeasurement(List<IonChamber> icList,
            List<List<DoseRateMeasurement>> measurementMatrix) {
        for (int j = 0; j < icList.size(); j++) {
            List<DoseRateMeasurement> measurementList = measurementMatrix.get(j);
            int lastIndex = measurementList.size() - 1;
            if (lastIndex >= 0) {
                measurementList.remove(lastIndex);
            }
        }
    }*/

    @Override
    public void notifyPvInfo(String pv, boolean couldConnect, DBRType type, Integer count,
            String[] enumLabels) {
        synchronized (fsdConnectLock) {
            fsdConnectLock.notifyAll();
        }
    }

    @Override
    public void notifyPvUpdate(String pv, DBR dbr) {
        double voltage = ((gov.aps.jca.dbr.DOUBLE) dbr).getDoubleValue()[0];

        LOGGER.log(Level.FINEST, "FSD Master Voltage: {0}", voltage);

        setFsdTripThisInstant(voltage != 0);

        if (voltage != 0) {
            setFsdTripDuringStep(true); // Notice we never clear, only set: user must accept notice first
        }
    }

    /**
     * After an FSD trip the resume command wakes the waiting worker thread so
     * that it can continue.
     */
    public void resume() {
        synchronized (fsdLock) {
            fsdLock.notifyAll();
        }
    }

    /**
     * The state of the worker.
     */
    public enum WorkerState {
        SETTLING("Settling"), DWELLING("Dwelling"), SAMPLING("Sampling"), ADJUSTING(
                "Adjusting Attenuator"), READING("Reading Current"), WAITING_ON_FSD("Waiting on FSD");
        /**
         * The operator friendly text for each state.
         */
        public final String label;

        WorkerState(String label) {
            this.label = label;
        }
    }

    /**
     * Captures the incremental result status of the worker thread.
     */
    public class IncrementalResultChunk {

        public WorkerState state;
        public int step;
        public Integer sample;

        public IncrementalResultChunk(WorkerState state, int step, Integer sample) {
            this.state = state;
            this.step = step;
            this.sample = sample;
        }
    }

    /**
     * The set of PVs for modifying an attenuator and reading back the current.
     */
    private class HallPvSet {

        private String hallAttenuatorPv;
        private String hallBeamCurrentReadbackPv;
        private String hallModePv;
    }
}
