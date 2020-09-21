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
import org.jlab.icalibrate.model.Hall;
import org.jlab.icalibrate.model.Laser;
import org.jlab.icalibrate.model.CreateNewDatasetParameters;
import org.jlab.icalibrate.wizard.Wizard;
import org.jlab.icalibrate.wizard.page.LaserTargetBeamPage;

/**
 * This is a SwingWorker which looks up the injector laser in use for the selected hall. The BOOM
 * Buddy PVs are consulted to determine the laser. Once the worker is complete it decrements the
 * count down latch such that the parent thread can determine when all concurrently executing child
 * threads are complete. The information gained from this worker is displayed on the
 * LaserTargetBeamPage.
 *
 * @author ryans
 */
public class LaserLookupWorker extends MinimumExecutionSwingWorker<Laser, Void> {

    private static final Logger LOGGER = Logger.getLogger(LaserLookupWorker.class.getName());

    private final Wizard<CreateNewDatasetParameters> wizard;
    private final LaserTargetBeamPage page;
    private final CountDownLatch doneLatch;

    public LaserLookupWorker(Wizard<CreateNewDatasetParameters> wizard, LaserTargetBeamPage page,
            CountDownLatch doneLatch) {
        this.wizard = wizard;
        this.page = page;
        this.doneLatch = doneLatch;
    }

    @Override
    protected Laser doWithMinimumExecution() throws Exception {
        Laser laser = null;

        CreateNewDatasetParameters params = wizard.getParameters();

        Hall hall = params.getHall();

        String laserAProperty;
        String laserBProperty;
        String laserCProperty;
        String laserDProperty;

        switch (hall) {
            case A:
                laserAProperty = "HALLA_LASERA_PV";
                laserBProperty = "HALLA_LASERB_PV";
                laserCProperty = "HALLA_LASERC_PV";
                laserDProperty = "HALLA_LASERD_PV";
                break;
            case C:
                laserAProperty = "HALLC_LASERA_PV";
                laserBProperty = "HALLC_LASERB_PV";
                laserCProperty = "HALLC_LASERC_PV";
                laserDProperty = "HALLC_LASERD_PV";
                break;
            case D:
                laserAProperty = "HALLD_LASERA_PV";
                laserBProperty = "HALLD_LASERB_PV";
                laserCProperty = "HALLD_LASERC_PV";
                laserDProperty = "HALLD_LASERD_PV";
                break;
            default:
                throw new IllegalArgumentException("Hall must be one of A, C, D");
        }

        String laserAPv = ICalibrateApp.APP_PROPERTIES.getProperty(laserAProperty);
        String laserBPv = ICalibrateApp.APP_PROPERTIES.getProperty(laserBProperty);
        String laserCPv = ICalibrateApp.APP_PROPERTIES.getProperty(laserCProperty);
        String laserDPv = ICalibrateApp.APP_PROPERTIES.getProperty(laserDProperty);

        if (laserAPv != null) {
            laserAPv = laserAPv.trim(); // Config file might have spaces!
        }

        if (laserBPv != null) {
            laserBPv = laserBPv.trim(); // Config file might have spaces!
        }

        if (laserCPv != null) {
            laserCPv = laserCPv.trim(); // Config file might have spaces!
        }

        if (laserDPv != null) {
            laserDPv = laserDPv.trim(); // Config file might have spaces!
        }

        ChannelManager manager = params.getChannelManager();

        DBR laserA;
        DBR laserB;
        DBR laserC;
        DBR laserD;

        try {
            laserA = manager.get(laserAPv);
        } catch (IllegalStateException e) {
            throw new AppException("Channel Access disconnected during EPICS CA Get of PV: "
                    + laserAPv
                    + " - " + e.getMessage(), e);
        } catch (CAException e) {
            throw new AppException("Unable to perform EPICS CA Get of PV: " + laserAPv
                    + " - " + e.getMessage(), e);
        } catch (TimeoutException e) {
            throw new AppException("Timeout waiting for EPICS CA Get of PV: " + laserAPv,
                    e);
        }

        try {
            laserB = manager.get(laserBPv);
        } catch (CAException e) {
            throw new AppException("Unable to perform EPICS CA Get of PV: " + laserAPv
                    + " - " + e.getMessage(), e);
        } catch (TimeoutException e) {
            throw new AppException("Timeout waiting for EPICS CA Get of PV: " + laserAPv,
                    e);
        }

        try {
            laserC = manager.get(laserCPv);
        } catch (CAException e) {
            throw new AppException("Unable to perform EPICS CA Get of PV: " + laserAPv
                    + " - " + e.getMessage(), e);
        } catch (TimeoutException e) {
            throw new AppException("Timeout waiting for EPICS CA Get of PV: " + laserAPv,
                    e);
        }

        try {
            laserD = manager.get(laserDPv);
        } catch (CAException e) {
            throw new AppException("Unable to perform EPICS CA Get of PV: " + laserAPv
                    + " - " + e.getMessage(), e);
        } catch (TimeoutException e) {
            throw new AppException("Timeout waiting for EPICS CA Get of PV: " + laserAPv,
                    e);
        }

        int aVal = ((gov.aps.jca.dbr.DBR_Enum) laserA).getEnumValue()[0];
        int bVal = ((gov.aps.jca.dbr.DBR_Enum) laserB).getEnumValue()[0];
        int cVal = ((gov.aps.jca.dbr.DBR_Enum) laserC).getEnumValue()[0];
        int dVal = ((gov.aps.jca.dbr.DBR_Enum) laserD).getEnumValue()[0];

        LOGGER.log(Level.FINEST, "Laser A: {0}", aVal);
        LOGGER.log(Level.FINEST, "Laser B: {0}", bVal);
        LOGGER.log(Level.FINEST, "Laser C: {0}", cVal);
        LOGGER.log(Level.FINEST, "Laser D: {0}", dVal);

        int total = aVal + bVal + cVal + dVal;

        if (total > 1) {
            throw new AppException("More than one laser assigned to hall "
                    + hall.name());
        }

        if (total < 1) {
            throw new AppException("No laser assigned to hall " + hall.name());
        }

        if (aVal == 1) {
            laser = Laser.A;
        } else if (bVal == 1) {
            laser = Laser.B;
        } else if (cVal == 1) {
            laser = Laser.C;
        } else if (dVal == 1) {
            laser = Laser.D;
        }

        return laser;
    }

    @Override
    protected void done() {
        try {
            Laser laser = get(); // See if there were any exceptions
            page.setLaser(laser);
        } catch (InterruptedException | ExecutionException ex) {
            String title = "Unable to lookup hall laser configuration";
            String message = "Unexpected error";
            LOGGER.log(Level.SEVERE, title, ex);

            page.setLaser(null);
            
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
            JOptionPane.showMessageDialog(wizard, message, title,
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            doneLatch.countDown();
        }
    }

}
