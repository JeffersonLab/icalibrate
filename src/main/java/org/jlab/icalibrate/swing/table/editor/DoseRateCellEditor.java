package org.jlab.icalibrate.swing.table.editor;

import java.awt.Component;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;
import javax.swing.DefaultCellEditor;
import javax.swing.JFormattedTextField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.NumberFormatter;

/**
 *
 * @author ryans
 */
public class DoseRateCellEditor extends DefaultCellEditor {

    public DoseRateCellEditor(JTextField textField) {
        super(textField);
    }

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
