package org.jlab.icalibrate.swing.document;

import java.awt.Toolkit;
import java.util.regex.Pattern;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

/**
 * A text document model for holding text that is size restricted.
 *
 * @author ryans
 */
public class SizeRestrictedDocument extends DefaultStyledDocument {

  /** The pattern. */
  private final Pattern pattern;

  /** The max size. */
  private final int max;

  /**
   * Create a new SizeRestrictedDocument.
   *
   * @param max The maximum number of characters allowed
   */
  public SizeRestrictedDocument(int max) {
    this.max = max;
    pattern = Pattern.compile("^.{0," + max + "}$");
  }

  @Override
  public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
    if (str != null && !str.isEmpty()) {
      String wouldbe = this.getText(0, this.getLength());
      StringBuilder builder = new StringBuilder(wouldbe);
      builder.insert(offs, str);
      if (pattern.matcher(builder.toString()).matches()) {
        super.insertString(offs, str, a);
      } else {
        Toolkit.getDefaultToolkit().beep();
        if (offs < max) {
          int limit = max - offs;
          String modified = str;
          if (str.length() > limit) {
            modified = str.substring(0, limit);
          }
          super.insertString(offs, modified, a);
        }
      }
    }
  }
}
