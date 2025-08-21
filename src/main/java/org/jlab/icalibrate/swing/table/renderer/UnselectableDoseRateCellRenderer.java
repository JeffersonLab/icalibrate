package org.jlab.icalibrate.swing.table.renderer;

import java.awt.Component;
import java.text.DecimalFormat;
import javax.swing.JLabel;
import javax.swing.JTable;

/**
 * Renders a table cell that contains a read-only unselectable dose rate. This renderer enforces a
 * right-aligned integer.
 *
 * @author ryans
 */
public class UnselectableDoseRateCellRenderer extends UnselectableCellRenderer {

  /** The formatter. */
  private final DecimalFormat formatter = new DecimalFormat("###,##0");

  /** Create a new UnselectableDoseRateCellRenderer. */
  public UnselectableDoseRateCellRenderer() {
    this.setHorizontalAlignment(JLabel.RIGHT);
  }

  @Override
  public Component getTableCellRendererComponent(
      JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

    if (value != null) {
      value = formatter.format(value);
    }

    return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
  }
}
