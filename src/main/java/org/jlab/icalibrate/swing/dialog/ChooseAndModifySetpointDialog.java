package org.jlab.icalibrate.swing.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JFormattedTextField;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableColumnModel;
import javax.swing.text.NumberFormatter;
import org.jlab.icalibrate.model.DoseRateTripSetpoint;
import org.jlab.icalibrate.model.IonChamber;
import org.jlab.icalibrate.swing.ICalibrateFrame;
import org.jlab.icalibrate.swing.table.model.ModifySetpointTableModel;
import org.jlab.icalibrate.swing.table.model.SetpointTableModel;
import org.jlab.icalibrate.swing.table.renderer.EditableDoseRateCellRenderer;
import org.jlab.icalibrate.swing.table.renderer.UnselectableDoseRateCellRenderer;
import org.jlab.icalibrate.swing.table.renderer.UnselectableCellRenderer;

/**
 * A JDialog which allows the user to choose which setpoints to export and to modify the setpoint
 * values as well.
 *
 * Layout was done using Netbeans Matisse Swing GUI builder.
 *
 * @author ryans
 */
public class ChooseAndModifySetpointDialog extends javax.swing.JDialog {

    private final ModifySetpointTableModel modifySetpointTableModel = new ModifySetpointTableModel();
    private final ICalibrateFrame frame;

    /**
     * Create a new ChooseAndModifySetpointDialog.
     *
     * @param parent The parent frame
     */
    public ChooseAndModifySetpointDialog(ICalibrateFrame parent) {
        super(parent, true);
        this.frame = parent;
        initComponents();
        initTable();
    }

    private void initTable() {
        setpointTable.getTableHeader()
                .setReorderingAllowed(false);
        setpointTable.getTableHeader()
                .setResizingAllowed(false);
        setpointTable.setRowSelectionAllowed(
                false);

        TableColumnModel setpointTableColumnModel = setpointTable.getColumnModel();

        UnselectableCellRenderer unselectableRenderer = new UnselectableCellRenderer();

        UnselectableDoseRateCellRenderer unselectableDoseRateCellRenderer
                = new UnselectableDoseRateCellRenderer();

        EditableDoseRateCellRenderer editableDoseRateCellRenderer
                = new EditableDoseRateCellRenderer();

        // Set header height due to multi-line headers
        setpointTable.getTableHeader()
                .setPreferredSize(new Dimension(125, 75));

        // Right align results table number columns
        setpointTableColumnModel.getColumn(0).setCellRenderer(unselectableRenderer); // Name        
        setpointTableColumnModel.getColumn(1).setCellRenderer(unselectableDoseRateCellRenderer); // Existing
        setpointTableColumnModel.getColumn(2).setCellRenderer(unselectableDoseRateCellRenderer); // Calculated
        setpointTableColumnModel.getColumn(3).setCellRenderer(editableDoseRateCellRenderer); // New Value

        setpointTableColumnModel.getColumn(0).setPreferredWidth(125); // Name
        setpointTableColumnModel.getColumn(1).setPreferredWidth(125); // Existing
        setpointTableColumnModel.getColumn(2).setPreferredWidth(125); // Calculated  
        setpointTableColumnModel.getColumn(3).setPreferredWidth(125); // New Value 
        setpointTableColumnModel.getColumn(4).setPreferredWidth(75); // Include

        setpointTableColumnModel.getColumn(0).setMinWidth(125); // Name
        setpointTableColumnModel.getColumn(1).setMinWidth(125); // Existing
        setpointTableColumnModel.getColumn(2).setMinWidth(125); // Calculated    
        setpointTableColumnModel.getColumn(3).setMinWidth(125); // New Value
        setpointTableColumnModel.getColumn(4).setMinWidth(75); // Include

        //setpointTableColumnModel.getColumn(0).setMaxWidth(125); // Name
        setpointTableColumnModel.getColumn(1).setMaxWidth(125); // Existing
        setpointTableColumnModel.getColumn(2).setMaxWidth(125); // Calculated 
        setpointTableColumnModel.getColumn(3).setMaxWidth(125); // New Value  
        setpointTableColumnModel.getColumn(4).setMaxWidth(75); // Include

        final JFormattedTextField tf = new JFormattedTextField();
        setpointTableColumnModel.getColumn(3).setCellEditor(new DefaultCellEditor(tf) {
            /*@Override
            public Object getCellEditorValue() {
                Double value = null;
                String text = tf.getText();

                if (text != null) {
                    try {
                        Double num = Double.valueOf(text);
                        DecimalFormat formatter = new DecimalFormat("0.00");
                        text = formatter.format(num);
                        value = Double.valueOf(text);
                    } catch (Exception e) {
                        // Unable to convert... just keep as is
                    }
                }

                return value;
            }*/

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int row, int column) {
                JFormattedTextField editor
                        = (JFormattedTextField) super.getTableCellEditorComponent(table, value,
                                isSelected, row, column);

                if (value instanceof Number) {
                    Locale myLocale = Locale.getDefault();

                    NumberFormat numberFormatB = NumberFormat.getInstance(myLocale);
                    numberFormatB.setMaximumFractionDigits(0);
                    numberFormatB.setMinimumFractionDigits(0);
                    numberFormatB.setMinimumIntegerDigits(1);

                    editor.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(
                            new NumberFormatter(numberFormatB)));

                    editor.setHorizontalAlignment(SwingConstants.RIGHT);
                    editor.setValue(value);
                }
                return editor;
            }

            @Override
            public boolean stopCellEditing() {
                try {
                    // try to get the value
                    this.getCellEditorValue();
                    return super.stopCellEditing();
                } catch (Exception ex) {
                    return false;
                }

            }

            @Override
            public Object getCellEditorValue() {
                // get content of textField
                String str = (String) super.getCellEditorValue();
                if (str == null) {
                    return null;
                }

                if (str.length() == 0) {
                    return null;
                }

                // try to parse a number
                try {
                    ParsePosition pos = new ParsePosition(0);
                    Number n = NumberFormat.getInstance().parse(str, pos);
                    if (pos.getIndex() != str.length()) {
                        throw new ParseException(
                                "parsing incomplete", pos.getIndex());
                    }

                    // return an instance of column class
                    return n.doubleValue();

                } catch (ParseException pex) {
                    throw new RuntimeException(pex);
                }
            }
        }
        );

    }

    /**
     * Set the data.
     *
     * @param rows The data
     */
    public void setData(LinkedHashSet<SetpointTableModel.SetpointRow> rows) {
        LinkedHashSet<ModifySetpointTableModel.ModifySetpointRow> copied = new LinkedHashSet<>();

        if (rows != null) {
            for (SetpointTableModel.SetpointRow row : rows) {
                copied.add(new ModifySetpointTableModel.ModifySetpointRow(row.getIonChamber(),
                        row.getExisting(),
                        row.getCalculated(), row.getCalculated()));
            }
        }

        modifySetpointTableModel.setRows(copied);
    }

    /**
     * Return the modify setpoints table data.
     *
     * @return The table data
     */
    public LinkedHashSet<ModifySetpointTableModel.ModifySetpointRow> getData() {
        LinkedHashSet<ModifySetpointTableModel.ModifySetpointRow> rows = null;

        rows = modifySetpointTableModel.getRows();

        return rows;
    }

    /**
     * Get the selected and modified setpoints.
     *
     * @return The setpoints
     */
    public List<DoseRateTripSetpoint> getSetpoints() {
        List<DoseRateTripSetpoint> setpointList = new ArrayList<>();
        LinkedHashSet<ModifySetpointTableModel.ModifySetpointRow> rows
                = modifySetpointTableModel.getRows();

        if (rows != null) {
            for (ModifySetpointTableModel.ModifySetpointRow row : rows) {
                IonChamber ic = row.getIonChamber();
                Double newValue = row.getNewValue();

                if (newValue != null) {
                    DoseRateTripSetpoint setpoint = new DoseRateTripSetpoint(ic, newValue);
                    setpointList.add(setpoint);
                }
            }
        }

        return setpointList;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cancelSetpointsButton = new javax.swing.JButton();
        saveSetpointsButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        setpointTable = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Modify/Confirm Setpoints");
        setMinimumSize(new java.awt.Dimension(650, 325));
        setPreferredSize(new java.awt.Dimension(650, 325));

        cancelSetpointsButton.setText("Cancel");
        cancelSetpointsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelSetpointsButtonActionPerformed(evt);
            }
        });

        saveSetpointsButton.setText("Write to EPICS");

        setpointTable.setModel(modifySetpointTableModel);
        jScrollPane1.setViewportView(setpointTable);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(saveSetpointsButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelSetpointsButton))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 473, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 60, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelSetpointsButton)
                    .addComponent(saveSetpointsButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelSetpointsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelSetpointsButtonActionPerformed
        setVisible(false);
    }//GEN-LAST:event_cancelSetpointsButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelSetpointsButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton saveSetpointsButton;
    private javax.swing.JTable setpointTable;
    // End of variables declaration//GEN-END:variables

    /**
     * Set the action to take after choosing and modifying setpoints. This allows both the export to
     * Excel and export to EPICS actions to use the same form.
     *
     * @param action The action to perform
     */
    public void setSaveAction(Action action) {
        saveSetpointsButton.setAction(action);
    }
}
