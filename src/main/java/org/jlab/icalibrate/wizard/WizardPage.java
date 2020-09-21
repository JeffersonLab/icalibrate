package org.jlab.icalibrate.wizard;

import javax.swing.JPanel;
import org.jlab.icalibrate.exception.ValidationException;

/**
 * A Swing GUI wizard dialog page.
 *
 * @author ryans
 */
public abstract class WizardPage<T> extends JPanel {

    private final Wizard<T> wizard;
    private final String title;

    /**
     * Create a new WizardPage.
     * 
     * @param wizard The parent Wizard in which this page belongs
     * @param title The page title
     */
    public WizardPage(Wizard<T> wizard, String title) {
        this.wizard = wizard;
        this.title = title;
    }

    /**
     * Called when the page is displayed. This is a good place to query a data source for
     * information needed to display the page. There is no way to abort entering a page so the page
     * must be prepared to handle the case in which it is unable to query for the data it needs
     * (possibly by displaying a special message).
     */
    public abstract void enter();

    /**
     * Called when the page is hidden. If a ValidationException is thrown the page is not hidden and
     * remains the current page.
     *
     * @throws ValidationException If the form fails validation and the page cannot be left
     */
    public abstract void leave() throws ValidationException;

    /**
     * Reset the page for a new invocation. Reset is not called as pages are navigated, but instead
     * each time before displaying the wizard - this way the wizard can be reused.
     */
    public abstract void reset();

    /**
     * Return the page title.
     * 
     * @return The page title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Return the wizard in which this page belongs.
     * 
     * @return The wizard
     */
    public Wizard<T> getWizard() {
        return wizard;
    }

    /**
     * Return the shared parameters in which this wizard operates.
     * 
     * @return The parameters
     */
    public T getParameters() {
        return wizard.getParameters();
    }
}
