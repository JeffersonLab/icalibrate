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
public class ModifySetpointTableModel extends AbstractTableModel {

    private final List<String> columnNames = Arrays.asList(new String[]{"Ion Chamber",
        "<html><center>Existing<br/>Setpoint (rads/hr)</center></html>",
        "<html><center>Calculated<br/>Setpoint (rads/hr)</center></html>",
        "<html><center style=\"color: green;\">New<br/>Setpoint (rads/hr)<br/>[Editable]</center></html>",
        "Include"});
    private List<ModifySetpointRow> rows = new ArrayList<>();

    /**
     * Return the ordered set of rows.
     *
     * @return The list of rows
     */
    public LinkedHashSet<ModifySetpointRow> getRows() {
        // We make a copy
        // (bad performance, but we don't worry about outside changes)        
        return new LinkedHashSet<>(rows);
    }

    /**
     * Replace the list of rows with the specified ordered set.
     *
     * @param rows The list of rows
     */
    public void setRows(LinkedHashSet<ModifySetpointRow> rows) {
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
    public void addAll(LinkedHashSet<ModifySetpointRow> options) {
        this.rows.addAll(options);
        fireTableDataChanged();
    }

    /**
     * Remove all OptionRows from the list and return them.
     *
     * @return The list of OptionRows
     */
    public LinkedHashSet<ModifySetpointRow> removeAll() {
        LinkedHashSet<ModifySetpointRow> result = new LinkedHashSet<>(rows);
        rows.clear();
        fireTableDataChanged();
        return result;
    }

    /**
     * Add the OptionRow to the end of the list.
     *
     * @param row The OptionRow to add
     */
    public void add(ModifySetpointRow row) {
        rows.add(row);
        fireTableDataChanged();
    }

    /**
     * Remove the OptionRow at the specified index.
     *
     * @param index The index
     * @return The OptionRow or null if not found
     */
    public ModifySetpointRow remove(int index) {
        ModifySetpointRow result = rows.remove(index);
        fireTableDataChanged();
        return result;
    }

    /**
     * Return the index of the specified ModifySetpointRow.
     *
     * @param row The ModifySetpointRow to search for
     * @return The index of the row or -1 if not found
     */
    public int findRowIndex(ModifySetpointRow row) {
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
                c = Double.class;
                break;
            case 4:
                c = Boolean.class;
                break;
            default:
                c = Object.class;
        }

        return c;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        Boolean include = (Boolean) getValueAt(rowIndex, 4);
        boolean editable = (columnIndex == 4 || (columnIndex == 3 && include));

        return editable;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {

        // This collection may throw an IndexOutOfBoundsException
        ModifySetpointRow row = rows.get(rowIndex);

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
            case 1:
                row.setExisting((Double) value);
                fireTableCellUpdated(rowIndex, columnIndex);
                break;
            case 2:
                row.setCalculated((Double) value);
                fireTableCellUpdated(rowIndex, columnIndex);
                break;
            case 3:
                row.setNewValue((Double) value);
                fireTableCellUpdated(rowIndex, columnIndex);
                break;
            case 4:
                Boolean v = (Boolean) value;

                row.setIncluded(v);

                if (v) {
                    row.setNewValue((Double) this.getValueAt(rowIndex, 2));
                } else {
                    row.setNewValue(null);
                }
                fireTableCellUpdated(rowIndex, 3);
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
        ModifySetpointRow row = rows.get(rowIndex);

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
                value = row.getNewValue();
                break;
            case 4:
                value = row.getIncluded();
                break;
        }

        return value;
    }

    /**
     * A table row model entity for holding modifiable setpoints and related data.
     * <br>
     * <p>
     * <b>Note:</b> a static nested class is behaviorally a top-level class that has been nested in
     * another top-level class for packaging convenience. A nested class makes sense when it is
     * intended to only be used in the context of the containing class.
     * </p>
     */
    public static class ModifySetpointRow {

        private final IonChamber ic;
        private Double existing;
        private Double calculated;
        private Double newValue;
        private Boolean included;

        /**
         * Create a new row.
         *
         * @param ic The ion chamber
         * @param existing The existing trip setpoint value (rads/hr)
         * @param calculated The calculated trip setpoint value (rads/hr)
         * @param newValue The new value to save
         */
        public ModifySetpointRow(IonChamber ic, Double existing, Double calculated, Double newValue) {
            this.ic = ic;
            this.existing = existing;
            this.calculated = calculated;
            this.newValue = newValue;
            this.included = true;
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

        /**
         * Return the new value to export, which is modifiable by the operator.
         *
         * @return The new value
         */
        public Double getNewValue() {
            return newValue;
        }

        /**
         * Set the new value to export, which is modifiable by the operator.
         *
         * @param newValue The new value
         */
        public void setNewValue(Double newValue) {
            this.newValue = newValue;
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
