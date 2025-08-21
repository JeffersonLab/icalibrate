package org.jlab.icalibrate.swing.action.listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import org.jlab.icalibrate.swing.generated.ICalibrateFrame;
import org.jlab.icalibrate.swing.generated.dialog.ModifySampleDataDialog;

/**
 * Handle a save modified sample data request.
 * 
 * @author ryans
 */
public final class SaveDataModificationsActionListener implements ActionListener {

    private static final Logger LOGGER = Logger.getLogger(SaveDataModificationsActionListener.class.getName());

    private final ICalibrateFrame frame;
    private final ModifySampleDataDialog dialog;

    /**
     * Create a new OpenHCDActionListener.
     *
     * @param dialog The ModifySampleDataFrame
     * @param frame The ICalibrateFrame
     */
    public SaveDataModificationsActionListener(ModifySampleDataDialog dialog, ICalibrateFrame frame) {
        this.dialog = dialog;
        this.frame = frame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {       
        frame.updateSampleData(dialog.getData());
        dialog.dispose();
    }
}
