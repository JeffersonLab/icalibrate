package org.jlab.icalibrate.swing.table.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * A table model for holding setpoints that can be modified.
 *
 * @author ryans
 */
public class ModifyDataTableModel extends AbstractTableModel {

    private final List<String> columnNames = Arrays.asList("<html><center>Current<br/>(microAmps)</center></html>",
            "<html><center>Dose Rate<br/>(rads/hr)</center></html>");
    private List<ModifyDoseRateRow> rows = new ArrayList<>();

    /**
     * Create a new ModifyTableModel.
     */
    public ModifyDataTableModel() {}

    /**
     * Return the ordered set of rows.
     *
     * @return The list of rows
     */
    public LinkedHashSet<ModifyDoseRateRow> getRows() {
        // We make a copy
        // (bad performance, but we don't worry about outside changes)        
        return new LinkedHashSet<>(rows);
    }

    /**
     * Replace the list of rows with the specified ordered set.
     *
     * @param rows The list of rows
     */
    public void setRows(LinkedHashSet<ModifyDoseRateRow> rows) {
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
    public void addAll(LinkedHashSet<ModifyDoseRateRow> options) {
        this.rows.addAll(options);
        fireTableDataChanged();
    }

    /**
     * Remove all OptionRows from the list and return them.
     *
     * @return The list of OptionRows
     */
    public LinkedHashSet<ModifyDoseRateRow> removeAll() {
        LinkedHashSet<ModifyDoseRateRow> result = new LinkedHashSet<>(rows);
        rows.clear();
        fireTableDataChanged();
        return result;
    }

    /**
     * Add the OptionRow to the end of the list.
     *
     * @param row The OptionRow to add
     */
    public void add(ModifyDoseRateRow row) {
        rows.add(row);
        fireTableDataChanged();
    }

    /**
     * Remove the OptionRow at the specified index.
     *
     * @param index The index
     * @return The OptionRow or null if not found
     */
    public ModifyDoseRateRow remove(int index) {
        ModifyDoseRateRow result = null;
        if (index >= 0) {
            result = rows.remove(index);
            fireTableDataChanged();
        }
        return result;
    }

    /**
     * Return the index of the specified ModifySetpointRow.
     *
     * @param row The ModifySetpointRow to search for
     * @return The index of the row or -1 if not found
     */
    public int findRowIndex(ModifyDoseRateRow row) {
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
                c = Double.class;
                break;
            case 1:
                c = Double.class;
                break;
            default:
                c = Object.class;
        }

        return c;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {

        // This collection may throw an IndexOutOfBoundsException
        ModifyDoseRateRow row = rows.get(rowIndex);

        // This collection may throw an IndexOutOfBoundsException
        columnNames.get(columnIndex);

        /*if (value instanceof String) {
            try {
                value = Double.valueOf((String) value);
            } catch (Exception e) {
                value = null;
            }
        }*/
        switch (columnIndex) {
            case 0:
                row.setCurrent((Double) value);
                fireTableCellUpdated(rowIndex, columnIndex);
                break;
            case 1:
                row.setDoseRateRadsPerHour((Double) value);
                fireTableCellUpdated(rowIndex, columnIndex);
                break;
            default:
                break;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Object value = null;

        // This collection may throw an IndexOutOfBoundsException
        ModifyDoseRateRow row = rows.get(rowIndex);

        // This collection may throw an IndexOutOfBoundsException
        String column = columnNames.get(columnIndex);

        switch (columnIndex) {
            case 0:
                value = row.getCurrent();
                break;
            case 1:
                value = row.getDoseRateRadsPerHour();
                break;
        }

        return value;
    }

    /**
     * A table row model entity for holding modifiable dose rates.
     * <br>
     * <p>
     * <b>Note:</b> a static nested class is behaviorally a top-level class that
     * has been nested in another top-level class for packaging convenience. A
     * nested class makes sense when it is intended to only be used in the
     * context of the containing class.
     * </p>
     */
    public static class ModifyDoseRateRow {

        private double current;
        private double doseRateRadsPerHour;

        /**
         * Create a new row.
         *
         * @param current The current (caller must keep up with units)
         * @param doseRateRadsPerHour The dose rate in rads per hour
         */
        public ModifyDoseRateRow(double current, double doseRateRadsPerHour) {
            this.current = current;
            this.doseRateRadsPerHour = doseRateRadsPerHour;
        }

        /**
         * Return the current (units are determined by caller).
         *
         * @return The current
         */
        public double getCurrent() {
            return current;
        }

        /**
         * Return the does rate in rads per hour.
         *
         * @return The dose rate
         */
        public double getDoseRateRadsPerHour() {
            return doseRateRadsPerHour;
        }

        /**
         * Set the current.
         *
         * @param current The current
         */
        public void setCurrent(double current) {
            this.current = current;
        }

        /**
         * Set the dose rate in rads per hour.
         *
         * @param doseRateRadsPerHour The dose rate
         */
        public void setDoseRateRadsPerHour(double doseRateRadsPerHour) {
            this.doseRateRadsPerHour = doseRateRadsPerHour;
        }
    }
}
