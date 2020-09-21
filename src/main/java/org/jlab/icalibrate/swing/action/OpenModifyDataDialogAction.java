package org.jlab.icalibrate.swing.action;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.jlab.icalibrate.model.ChartDataset;
import org.jlab.icalibrate.swing.ICalibrateFrame;
import org.jlab.icalibrate.swing.dialog.ModifySampleDataDialog;
import org.jlab.icalibrate.swing.util.DoseRateChartPanel;

/**
 *
 * @author ryans
 */
public class OpenModifyDataDialogAction extends AbstractAction {

    private final DoseRateChartPanel chartPanel;
    private final ModifySampleDataDialog modifyDoseRateDialog;
    private final ICalibrateFrame frame;

    public OpenModifyDataDialogAction(DoseRateChartPanel chartPanel, ModifySampleDataDialog modifyDoseRateDialog, ICalibrateFrame frame) {
        this.chartPanel = chartPanel;
        this.modifyDoseRateDialog = modifyDoseRateDialog;
        this.frame = frame;
        
        putValue(AbstractAction.NAME, "Modify Data");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ChartDataset selected = chartPanel.getDataset();
        modifyDoseRateDialog.setTitle("Modify Data: " + selected.getMeasuredDataset().getIonChamber().getFullName());
        modifyDoseRateDialog.setData(selected.getMeasuredDataset().getMeasurementList());
        modifyDoseRateDialog.pack();
        modifyDoseRateDialog.setLocationRelativeTo(frame);
        modifyDoseRateDialog.setVisible(true);
    }

}
