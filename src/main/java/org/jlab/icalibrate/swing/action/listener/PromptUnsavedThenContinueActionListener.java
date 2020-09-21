package org.jlab.icalibrate.swing.action.listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.jlab.icalibrate.swing.ICalibrateFrame;

/**
 * Handle an action request that first requires checking if unsaved data exists and if so prompts
 * the user for a decision.
 *
 * @author ryans
 */
public final class PromptUnsavedThenContinueActionListener implements ActionListener {

    private static final Logger LOGGER = Logger.getLogger(
            PromptUnsavedThenContinueActionListener.class.getName());

    private final ICalibrateFrame frame;
    private final ActionListener continueAction;

    /**
     * Create a new PromptUnsavedThenContinueActionListener.
     *
     * @param frame The ICalibrateFrame
     * @param continueAction The action to perform after continuing
     */
    public PromptUnsavedThenContinueActionListener(ICalibrateFrame frame,
            ActionListener continueAction) {
        this.frame = frame;
        this.continueAction = continueAction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (frame.isUnsavedData()) {
            // We use buttons instead of text to force button width to NOT all match
            final JButton cancelButton = new JButton("Cancel");
            final JButton doNotSaveButton = new JButton("Don't Save");
            cancelButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    SwingUtilities.getWindowAncestor(cancelButton).dispose();
                }
            });
            doNotSaveButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SwingUtilities.getWindowAncestor(cancelButton).dispose();
                    doContinueAction();
                }
            });
            Object[] options = new Object[]{"Save", doNotSaveButton, cancelButton};
            // JOptionPane will create a modal dialog that pauses code execution here, yet magically does not freeze EDT
            int response = JOptionPane.showOptionDialog(frame,
                    "Do you want to save the dataset?",
                    "Unsaved Dataset", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

            if (response == 0) { // Save
                SaveHCDThenContinueActionListener saveAction
                        = new SaveHCDThenContinueActionListener(frame,
                                continueAction);
                saveAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                        "Continue"));
            }
        } else {
            doContinueAction();
        }
    }

    private void doContinueAction() {
        if (continueAction != null) {
            continueAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                    "Continue"));
        }
    }
}
