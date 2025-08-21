package org.jlab.icalibrate.swing.table.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.jlab.icalibrate.model.IonChamber;

/**
 * A table model for holding read-only setpoints.
 *
 * @author ryans
 */
public class SetpointTableModel extends AbstractTableModel {

    /**
     * The column names list.
     */
    private final List<String> columnNames = Arrays.asList("Ion Chamber",
            "<html><center>Existing<br/>Setpoint (rads/hr)</center></html>",
            "<html><center>Calculated<br/>Setpoint (rads/hr)</center></html>",
            "<html><center>Log<br/>Fit</center></html>");

    /**
     * The rows.
     */
    private List<SetpointRow> rows = new ArrayList<>();

    /**
     * Create a new SetpointTableModel.
     */
    public SetpointTableModel() {
    }

    /**
     * Return the ordered set of rows.
     *
     * @return The list of rows
     */
    public LinkedHashSet<SetpointRow> getRows() {
        // We make a copy
        // (bad performance, but we don't worry about outside changes)        
        return new LinkedHashSet<>(rows);
    }

    /**
     * Replace the list of rows with the specified ordered set.
     *
     * @param rows The list of rows
     */
    public void setRows(LinkedHashSet<SetpointRow> rows) {
        // We make a copy 
        // (bad performance, but we don't worry about outside changes)
        // We also store internally as list, but require a linkedhashset
        // to ensure it at least comes in unique and ordered
        this.rows = new ArrayList<>(rows);
        fireTableDataChanged();
    }

    /**
     * Add the specified list of OptionRows to the existing list.
     *
     * @param options The list of OptionRows to add
     */
    public void addAll(LinkedHashSet<SetpointRow> options) {
        this.rows.addAll(options);
        fireTableDataChanged();
    }

    /**
     * Remove all OptionRows from the list and return them.
     *
     * @return The list of OptionRows
     */
    public LinkedHashSet<SetpointRow> removeAll() {
        LinkedHashSet<SetpointRow> result = new LinkedHashSet<>(rows);
        rows.clear();
        fireTableDataChanged();
        return result;
    }

    /**
     * Add the OptionRow to the end of the list.
     *
     * @param row The OptionRow to add
     */
    public void add(SetpointRow row) {
        rows.add(row);
        fireTableDataChanged();
    }

    /**
     * Remove the OptionRow at the specified index.
     *
     * @param index The index
     * @return The OptionRow or null if not found
     */
    public SetpointRow remove(int index) {
        SetpointRow result = rows.remove(index);
        fireTableDataChanged();
        return result;
    }

    /**
     * Return the index of the specified SetpointRow.
     *
     * @param row The SetpointRow to search for
     * @return The index of the row or -1 if not found
     */
    public int findRowIndex(SetpointRow row) {
        return rows.indexOf(row);
    }

    /**
     * Returns the column name at the specified index.
     *
     * @param columnIndex The column index
     * @return The column name
     */
    @Override
    public String getColumnName(int columnIndex) {
        return columnNames.get(columnIndex);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        Class c;

        switch (columnIndex) {
            case 0:
                c = String.class;
                break;
            case 1:
                c = Double.class;
                break;
            case 2:
                c = Double.class;
                break;
            case 3:
                c = Boolean.class;
                break;
            default:
                c = Object.class;
        }

        return c;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return (columnIndex == 3);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {

        // This collection may throw an IndexOutOfBoundsException
        SetpointRow row = rows.get(rowIndex);

        // This collection may throw an IndexOutOfBoundsException
        columnNames.get(columnIndex);

        if (columnIndex == 1) {
            row.setExisting((Double) value);
            fireTableCellUpdated(rowIndex, columnIndex);
        } else if (columnIndex == 2) {
            row.setCalculated((Double) value);
            fireTableCellUpdated(rowIndex, columnIndex);
        } else if (columnIndex == 3) {
            row.setLogarithmic((Boolean) value);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    /**
     * Set all rows to logarithmic.
     */
    public void setLogAll() {
        for (int i = 0; i < rows.size(); i++) {
            SetpointRow row = rows.get(i);
            row.setLogarithmic(true);
            fireTableCellUpdated(i, 3);
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Object value = null;

        // This collection may throw an IndexOutOfBoundsException
        SetpointRow row = rows.get(rowIndex);

        // This collection may throw an IndexOutOfBoundsException
        String column = columnNames.get(columnIndex);

        switch (columnIndex) {
            case 0:
                value = row.getIonChamber().getFriendlyNameOrEpicsName();
                break;
            case 1:
                value = row.getExisting();
                break;
            case 2:
                value = row.getCalculated();
                break;
            case 3:
                value = row.isLogarithmic();
                break;
        }

        return value;
    }

    /**
     * A table row model entity for holding read-only setpoints and related
     * data.
     * <br>
     * <p>
     * <b>Note:</b> a static nested class is behaviorally a top-level class that
     * has been nested in another top-level class for packaging convenience. A
     * nested class makes sense when it is intended to only be used in the
     * context of the containing class.
     * </p>
     */
    public static class SetpointRow {

        private final IonChamber ic;
        private Double existing;
        private Double calculated;
        private Boolean logarithmic = false;

        /**
         * Create a new row.
         *
         * @param ic The ion chamber name
         * @param existing The existing trip setpoint value (rads/hr)
         * @param calculated The calculated trip setpoint value (rads/hr)
         */
        public SetpointRow(IonChamber ic, Double existing, Double calculated) {
            this.ic = ic;
            this.existing = existing;
            this.calculated = calculated;
        }

        /**
         * Return the ion chamber.
         *
         * @return The ion chamber
         */
        public IonChamber getIonChamber() {
            return ic;
        }

        /**
         * Return the existing setpoint in rads per hour.
         *
         * @return The existing setpoint
         */
        public Double getExisting() {
            return existing;
        }

        /**
         * Set the existing setpoint in rads per hour.
         *
         * @param existing The existing setpoint
         */
        public void setExisting(Double existing) {
            this.existing = existing;
        }

        /**
         * Return the calculated setpoint in rads per hour.
         *
         * @return The calculated setpoint
         */
        public Double getCalculated() {
            return calculated;
        }

        /**
         * Set the calculated setpoint in rads per hour.
         *
         * @param calculated The calculated setpoint
         */
        public void setCalculated(Double calculated) {
            this.calculated = calculated;
        }

        private void setLogarithmic(Boolean logarithmic) {
            this.logarithmic = logarithmic;
        }

        private Boolean isLogarithmic() {
            return logarithmic;
        }
    }
}
