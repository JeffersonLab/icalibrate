package org.jlab.icalibrate.swing;

import java.awt.EventQueue;
import java.awt.Frame;
import javax.swing.JDialog;

import org.jlab.icalibrate.swing.generated.dialog.WaitDialog;
import org.jlab.icalibrate.swing.util.FrostedGlassPane;

/**
 * A JDialog which supports showing a "Please wait" message with a spinning icon during long running
 * operations in which the user should not be allowed to do anything but wait.
 *
 * @author ryans
 */
public class ModalWaitDialog extends JDialog {

    private final FrostedGlassPane frostedPane = new FrostedGlassPane();
    private final WaitDialog waitDialog = new WaitDialog(this);

    {
        setGlassPane(frostedPane);
    }

    /**
     * Create a new ModalWaitDialog.
     *
     * @param owner The owner Frame
     * @param modal true if modal
     */
    public ModalWaitDialog(Frame owner, boolean modal) {
        super(owner, modal);
    }

    /**
     * Show the wait dialog at the earliest opportunity.
     */
    public void queueShowModalWait() {
        frostedPane.setVisible(true);

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                waitDialog.pack();
                waitDialog.setLocationRelativeTo(ModalWaitDialog.this);
                waitDialog.setVisible(true);
            }
        });
    }

    /**
     * Hide the wait dialog.
     */
    public void hideModalWait() {
        waitDialog.setVisible(false);
        frostedPane.setVisible(false);
    }
}
