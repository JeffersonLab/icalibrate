package org.jlab.icalibrate.swing.worker;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.jlab.icalibrate.exception.AppException;
import org.jlab.icalibrate.model.Hall;
import org.jlab.icalibrate.model.CreateNewDatasetParameters;
import org.jlab.icalibrate.model.IonChamber;
import org.jlab.icalibrate.wizard.Wizard;
import org.jlab.icalibrate.wizard.page.IonChamberPage;

/**
 * This is a SwingWorker which looks up the ion chambers for the selected hall.
 *
 * @author ryans
 */
public class IonChamberLookupWorker extends MinimumExecutionSwingWorker<List<IonChamber>, Void> {

    private static final Logger LOGGER = Logger.getLogger(IonChamberLookupWorker.class.getName());

    private final Wizard<CreateNewDatasetParameters> wizard;
    private final IonChamberPage page;

    /**
     * Create a new IonChamberLookupWorker.
     *
     * @param wizard The wizard
     * @param page The page
     */
    public IonChamberLookupWorker(Wizard<CreateNewDatasetParameters> wizard, IonChamberPage page) {
        this.wizard = wizard;
        this.page = page;
    }

    @Override
    protected List<IonChamber> doWithMinimumExecution() throws Exception {

        CreateNewDatasetParameters params = wizard.getParameters();

        Hall hall = params.getHall();

        List<IonChamber> icList = IonChamber.createIcs(hall);

        return icList;
    }

    @Override
    protected void done() {
        try {
            List<IonChamber> icList = get(); // See if there were any exceptions
            page.setIonChambers(icList);
        } catch (InterruptedException | ExecutionException ex) {
            page.setIonChambers(null);
            String title = "Unable to lookup ion chambers";
            String message = "Unable to lookup ion chambers";
            LOGGER.log(Level.SEVERE, title, ex);

            Throwable cause = ex.getCause();
            if (cause != null) {
                if (cause instanceof AppException) {
                    message = cause.getMessage();
                }
            }
            JOptionPane.showMessageDialog(wizard, message, title,
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            wizard.hideModalWait();
        }
    }
}
