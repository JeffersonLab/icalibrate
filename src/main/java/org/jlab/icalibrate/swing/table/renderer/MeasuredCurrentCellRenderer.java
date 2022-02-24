package org.jlab.icalibrate.swing.table.renderer;

import java.awt.Component;
import java.text.DecimalFormat;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renders a table cell that contains a measured current. This renderer enforces a right-aligned
 * floating point number with exactly two numbers to the right of the decimal.
 *
 * @author ryans
 */
public class MeasuredCurrentCellRenderer extends DefaultTableCellRenderer {

    private final DecimalFormat formatter = new DecimalFormat("###,###,##0.00");

    {
        this.setHorizontalAlignment(JLabel.RIGHT);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        if (value != null) {
            value = formatter.format(value);
        }

        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
}
