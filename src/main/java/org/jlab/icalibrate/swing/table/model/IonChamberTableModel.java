package org.jlab.icalibrate.swing.table.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.jlab.icalibrate.model.IonChamber;

/**
 * A table model for holding setpoints that can be modified.
 *
 * @author ryans
 */
public class IonChamberTableModel extends AbstractTableModel {

    private final List<String> columnNames = Arrays.asList(new String[]{"Name",
        "EPICS Name",
        "Include"});
    private List<IonChamberRow> rows = new ArrayList<>();

    /**
     * Return the ordered set of rows.
     *
     * @return The list of rows
     */
    public LinkedHashSet<IonChamberRow> getRows() {
        // We make a copy
        // (bad performance, but we don't worry about outside changes)        
        return new LinkedHashSet<>(rows);
    }

    /**
     * Replace the list of rows with the specified ordered set.
     *
     * @param rows The list of rows
     */
    public void setRows(LinkedHashSet<IonChamberRow> rows) {
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
    public void addAll(LinkedHashSet<IonChamberRow> options) {
        this.rows.addAll(options);
        fireTableDataChanged();
    }

    /**
     * Remove all OptionRows from the list and return them.
     *
     * @return The list of OptionRows
     */
    public LinkedHashSet<IonChamberRow> removeAll() {
        LinkedHashSet<IonChamberRow> result = new LinkedHashSet<>(rows);
        rows.clear();
        fireTableDataChanged();
        return result;
    }

    /**
     * Add the row to the end of the list.
     *
     * @param row The row to add
     */
    public void add(IonChamberRow row) {
        rows.add(row);
        fireTableDataChanged();
    }

    /**
     * Remove the OptionRow at the specified index.
     *
     * @param index The index
     * @return The OptionRow or null if not found
     */
    public IonChamberRow remove(int index) {
        IonChamberRow result = rows.remove(index);
        fireTableDataChanged();
        return result;
    }

    /**
     * Return the index of the specified row.
     *
     * @param row The IonChamberRow to search for
     * @return The index of the row or -1 if not found
     */
    public int findRowIndex(IonChamberRow row) {
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
                c = String.class;
                break;
            case 2:
                c = Boolean.class;
                break;
            default:
                c = Object.class;
        }

        return c;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        boolean editable = (columnIndex == 2);

        return editable;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {

        // This collection may throw an IndexOutOfBoundsException
        IonChamberRow row = rows.get(rowIndex);

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
            case 2:
                Boolean v = (Boolean) value;

                row.setIncluded(v);

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
        IonChamberRow row = rows.get(rowIndex);

        // This collection may throw an IndexOutOfBoundsException
        String column = columnNames.get(columnIndex);

        switch (columnIndex) {
            case 0:
                value = row.getIonChamber().getFriendlyName();
                break;
            case 1:
                value = row.getIonChamber().getEpicsName();
                break;
            case 2:
                value = row.getIncluded();
                break;
        }

        return value;
    }

    /**
     * A table row model entity for holding ion chambers.
     * <br>
     * <p>
     * <b>Note:</b> a static nested class is behaviorally a top-level class that has been nested in
     * another top-level class for packaging convenience. A nested class makes sense when it is
     * intended to only be used in the context of the containing class.
     * </p>
     */
    public static class IonChamberRow {

        private final IonChamber ic;
        private Boolean included;

        /**
         * Create a new row.
         *
         * @param ic The ion chamber
         * @param included true to include, false otherwise
         */
        public IonChamberRow(IonChamber ic, boolean included) {
            this.ic = ic;
            this.included = included;
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
         * Return whether the row has been selected by the operator to be exported.
         *
         * @return true if included, false otherwise
         */
        public Boolean getIncluded() {
            return included;
        }

        /**
         * Set whether the row has been selected by the operator to be exported.
         *
         * @param included true if included, false otherwise
         */
        public void setIncluded(Boolean included) {
            this.included = included;
        }
    }
}
