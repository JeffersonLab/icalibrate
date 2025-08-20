package org.jlab.icalibrate.swing;

import java.awt.EventQueue;
import javax.swing.JFrame;
import org.jlab.icalibrate.swing.dialog.WaitDialog;
import org.jlab.icalibrate.swing.util.FrostedGlassPane;

/**
 * A JFrame which supports showing a "Please wait" message with a spinning icon during long running
 * operations in which the user should not be allowed to do anything but wait.
 * 
 * @author ryans
 */
public class ModalWaitFrame extends JFrame {

    /**
     * The frosted pane.
     */
    private final FrostedGlassPane frostedPane = new FrostedGlassPane();

    /**
     * The wait dialog.
     */
    private final WaitDialog waitDialog = new WaitDialog(this);

    /**
     * Create a new ModalWaitFrame.
     */
    public ModalWaitFrame()
    {
        setGlassPane(frostedPane);    
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
                waitDialog.setLocationRelativeTo(ModalWaitFrame.this);
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
