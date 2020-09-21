package org.jlab.icalibrate.swing.table.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.jlab.icalibrate.model.DoseRateMeasurement;

/**
 * A table model for holding dose rate measurements.
 *
 * @author ryans
 */
public class DoseRateTableModel extends AbstractTableModel {

    private final List<String> columnNames = Arrays.asList(new String[]{"<html><center>Current<br/>(microAmps)</center></html>",
        "<html><center>Dose Rate<br/>(rads/hr)</center></html>"});
    List<DoseRateMeasurement> rows = new ArrayList<>();
    
    /**
     * Return the ordered set of rows.
     *
     * @return The list of rows
     */
    public LinkedHashSet<DoseRateMeasurement> getRows() {
        // We make a copy
        // (bad performance, but we don't worry about outside changes)        
        return new LinkedHashSet<>(rows);
    }

    public void setCurrentUnits(String units) {
        columnNames.set(0, "<html><center>Current<br/>(" + units + ")</center></html>");
        fireTableStructureChanged(); // This undoes all of the cell sizing and cell renderer config!
    }
    
    /**
     * Replace the list of rows with the specified ordered set.
     *
     * @param rows The list of rows
     */
    public void setRows(LinkedHashSet<DoseRateMeasurement> rows) {
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
    public void addAll(LinkedHashSet<DoseRateMeasurement> options) {
        this.rows.addAll(options);
        fireTableDataChanged();
    }

    /**
     * Remove all OptionRows from the list and return them.
     *
     * @return The list of OptionRows
     */
    public LinkedHashSet<DoseRateMeasurement> removeAll() {
        LinkedHashSet<DoseRateMeasurement> result = new LinkedHashSet<>(rows);
        rows.clear();
        fireTableDataChanged();
        return result;
    }

    /**
     * Add the OptionRow to the end of the list.
     *
     * @param row The OptionRow to add
     */
    public void add(DoseRateMeasurement row) {
        rows.add(row);
        fireTableDataChanged();
    }

    /**
     * Remove the OptionRow at the specified index.
     *
     * @param index The index
     * @return The OptionRow or null if not found
     */
    public DoseRateMeasurement remove(int index) {
        DoseRateMeasurement result = rows.remove(index);
        fireTableDataChanged();
        return result;
    }

    /**
     * Return the index of the specified DoseRateMeasurement.
     *
     * @param row The DoseRateMeasurement to search for
     * @return The index of the row or -1 if not found
     */
    public int findRowIndex(DoseRateMeasurement row) {
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
        return false;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {

        // This collection may throw an IndexOutOfBoundsException
        DoseRateMeasurement row = rows.get(rowIndex);

        // This collection may throw an IndexOutOfBoundsException
        columnNames.get(columnIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Object value = null;

        // This collection may throw an IndexOutOfBoundsException
        DoseRateMeasurement row = rows.get(rowIndex);

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
}
