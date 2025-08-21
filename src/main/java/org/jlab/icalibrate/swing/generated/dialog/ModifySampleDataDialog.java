package org.jlab.icalibrate.swing.generated.dialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import javax.swing.Action;
import javax.swing.JFormattedTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;
import org.jlab.icalibrate.model.DoseRateMeasurement;
import org.jlab.icalibrate.swing.generated.ICalibrateFrame;
import org.jlab.icalibrate.swing.action.listener.SaveDataModificationsActionListener;
import org.jlab.icalibrate.swing.table.editor.CurrentCellEditor;
import org.jlab.icalibrate.swing.table.editor.DoseRateCellEditor;
import org.jlab.icalibrate.swing.table.model.ModifyDataTableModel;
import org.jlab.icalibrate.swing.table.model.ModifyDataTableModel.ModifyDoseRateRow;
import org.jlab.icalibrate.swing.table.renderer.EditableDoseRateCellRenderer;
import org.jlab.icalibrate.swing.table.renderer.MeasuredCurrentCellRenderer;

/**
 * A JDialog which allows the user to choose which setpoints to export and to
 * modify the setpoint values as well.
 *
 * Layout was done using Netbeans Matisse Swing GUI builder.
 *
 * @author ryans
 */
public class ModifySampleDataDialog extends javax.swing.JDialog {
    
    private final ModifyDataTableModel modifyDoseRateTableModel = new ModifyDataTableModel();
    private final ICalibrateFrame frame;

    /**
     * Create a new ChooseAndModifySetpointDialog.
     *
     * @param parent The parent frame
     */
    public ModifySampleDataDialog(ICalibrateFrame parent) {
        super(parent, true);
        this.frame = parent;
        initComponents();
        initActions();
        initTable();
    }
    
    private void initActions() {
        this.saveDoseRatesButton.addActionListener(new SaveDataModificationsActionListener(this, frame));
        
        this.addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ModifyDoseRateRow row = new ModifyDoseRateRow(0, 0);
                modifyDoseRateTableModel.add(row);
            }
        });
        
        this.deleteButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                modifyDoseRateTableModel.remove(doseRateTable.getSelectedRow());
            }
        });
    }
    
    private void initTable() {
        doseRateTable.getTableHeader()
                .setReorderingAllowed(false);
        doseRateTable.getTableHeader()
                .setResizingAllowed(false);
        doseRateTable.setRowSelectionAllowed(
                true);
        doseRateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        TableColumnModel doseRateTableColumnModel = doseRateTable.getColumnModel();

        // Set header height due to multi-line headers
        doseRateTable.getTableHeader()
                .setPreferredSize(new Dimension(125, 50));

        // Right align results table number columns
        doseRateTableColumnModel.getColumn(0).setCellRenderer(new MeasuredCurrentCellRenderer()); // Current        
        doseRateTableColumnModel.getColumn(1).setCellRenderer(new EditableDoseRateCellRenderer()); // Dose Rate

        doseRateTableColumnModel.getColumn(0).setPreferredWidth(125); // Current
        doseRateTableColumnModel.getColumn(1).setPreferredWidth(125); // Dose Rate

        doseRateTableColumnModel.getColumn(0).setMinWidth(125); // Current
        doseRateTableColumnModel.getColumn(1).setMinWidth(125); // Dose Rate

        //doseRateTableColumnModel.getColumn(0).setMaxWidth(125); // Current
        //doseRateTableColumnModel.getColumn(1).setMaxWidth(125); // Dose Rate
        doseRateTableColumnModel.getColumn(0).setCellEditor(new CurrentCellEditor(new JFormattedTextField()));
        doseRateTableColumnModel.getColumn(1).setCellEditor(new DoseRateCellEditor(new JFormattedTextField()));
    }

    /**
     * Set the data.
     *
     * @param rows The data
     */
    public void setData(List<DoseRateMeasurement> rows) {
        LinkedHashSet<ModifyDataTableModel.ModifyDoseRateRow> copied = new LinkedHashSet<>();

        // TODO: We should really implement the hashcode method of ModifyDoseRateRow if we are going to be using a LinkedHashSet.
        if (rows != null) {
            for (DoseRateMeasurement row : rows) {
                copied.add(new ModifyDataTableModel.ModifyDoseRateRow(row.getCurrent(), row.getDoseRateRadsPerHour()));
            }
        }
        
        modifyDoseRateTableModel.setRows(copied);
    }

    /**
     * Get the data.
     *
     * @return The data
     */
    public List<DoseRateMeasurement> getData() {
        List<DoseRateMeasurement> doseRateList = new ArrayList<>();
        LinkedHashSet<ModifyDataTableModel.ModifyDoseRateRow> rows
                = modifyDoseRateTableModel.getRows();
        
        if (rows != null) {
            for (ModifyDataTableModel.ModifyDoseRateRow row : rows) {
                double current = row.getCurrent();
                double doseRateRadsPerHour = row.getDoseRateRadsPerHour();
                
                doseRateList.add(new DoseRateMeasurement(current, doseRateRadsPerHour));
            }
        }
        
        return doseRateList;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cancelSetpointsButton = new javax.swing.JButton();
        saveDoseRatesButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        doseRateTable = new javax.swing.JTable();
        addButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Modify Data");
        setMinimumSize(new java.awt.Dimension(450, 425));
        setPreferredSize(new java.awt.Dimension(450, 425));

        cancelSetpointsButton.setText("Cancel");
        cancelSetpointsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelSetpointsButtonActionPerformed(evt);
            }
        });

        saveDoseRatesButton.setText("OK");
        saveDoseRatesButton.setToolTipText("");

        doseRateTable.setModel(modifyDoseRateTableModel);
        doseRateTable.setToolTipText("");
        jScrollPane1.setViewportView(doseRateTable);

        addButton.setText("Append New Row");

        deleteButton.setText("Delete Selected Row");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(saveDoseRatesButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelSetpointsButton))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 473, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(deleteButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addButton)
                    .addComponent(deleteButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelSetpointsButton)
                    .addComponent(saveDoseRatesButton))
                .addContainerGap())
        );

        getAccessibleContext().setAccessibleDescription("");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelSetpointsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelSetpointsButtonActionPerformed
        setVisible(false);
    }//GEN-LAST:event_cancelSetpointsButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JButton cancelSetpointsButton;
    private javax.swing.JButton deleteButton;
    private javax.swing.JTable doseRateTable;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton saveDoseRatesButton;
    // End of variables declaration//GEN-END:variables

    /**
     * Set the action to take after choosing and modifying setpoints. This
     * allows both the export to Excel and export to EPICS actions to use the
     * same form.
     *
     * @param action The action to perform
     */
    public void setSaveAction(Action action) {
        saveDoseRatesButton.setAction(action);
    }
}
