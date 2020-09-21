package org.jlab.icalibrate;

import java.awt.EventQueue;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jlab.icalibrate.epics.ChannelManager;
import org.jlab.icalibrate.exception.InitializationException;
import org.jlab.icalibrate.exception.MissingDataException;
import org.jlab.icalibrate.file.io.DatasetFileReader;
import org.jlab.icalibrate.model.HallCalibrationDataset;
import org.jlab.icalibrate.swing.ICalibrateFrame;
import org.jlab.icalibrate.swing.MaydayFrame;

/**
 * The entry point of the application.
 *
 * The ICalibrateApp class loads the configuration properties, initializes a network connection to
 * EPICS, and initializes the Swing GUI.
 *
 * @author ryans
 */
public class ICalibrateApp {

    private static final Logger LOGGER = Logger.getLogger(
            ICalibrateApp.class.getName());

    /**
     * The application's configuration properties.
     */
    public static final Properties APP_PROPERTIES = new Properties();

    /**
     * Create a new ICalibrateApp.
     *
     * @param file The file to load on startup, or null if none
     * @param current The initial current to use for setpoint calculations, or null for the default
     * @throws InitializationException If unable to initialize the application
     * @throws IOException If unable to load configuration or connect to EPICS
     */
    public ICalibrateApp(File file, Integer current) throws InitializationException, IOException {
        ICalibrateFrame frame;

        try (ChannelManager channelManager = new ChannelManager()) {
            frame = new ICalibrateFrame(channelManager);

            if (current != null) {
                frame.setCurrentParameter(current);
            }

            if (file != null) {
                try {
                    DatasetFileReader reader = new DatasetFileReader();
                    HallCalibrationDataset ds = reader.read(file);
                    frame.setDataset(ds, file.getName(), true);
                } catch (IOException | ParseException e) {
                    throw new InitializationException("Unable to load file: " + e.getMessage(), e);
                } catch (MissingDataException e) {
                    throw new InitializationException("Unable to load file " + file.getName()
                            + ": Ion Chambers do not match current configuration", e);
                }
            }

            show(frame);

            LOGGER.log(Level.FINEST, "Waiting for frame to close");

            // Wait for frame to close
            try {
                synchronized (frame) {
                    frame.wait();
                }
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "wait interrupted");
            }

            LOGGER.log(Level.FINEST, "Frame has closed");
        }
    }

    private static void show(final Frame frame) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

    /**
     * The main method is the entry method of the application.
     *
     * @param args The program arguments
     */
    public static void main(String[] args) {
        File file = null;
        Integer current = null;

        if (args.length > 0) {
            file = new File(args[0]);
        }

        if (args.length > 1) {
            current = Integer.parseInt(args[1]);
        }

        try (InputStream propStream
                = ICalibrateApp.class.getClassLoader().getResourceAsStream(
                        "icalibrate.properties")) {
            if (propStream == null) {
                throw new InitializationException(
                        "File Not Found; Configuration File: icalibrate.properties");
            }

            APP_PROPERTIES.load(propStream);

            new ICalibrateApp(file, current);

            LOGGER.log(Level.FINEST, "Shutdown completed successfully");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to load configuration file",
                    e);
            MaydayFrame mayday = new MaydayFrame(
                    "Unable to load configuration file: " + e.getMessage());
            show(mayday);
        } catch (InitializationException e) {
            LOGGER.log(Level.SEVERE, "Unable to initialize", e);
            MaydayFrame mayday = new MaydayFrame(
                    "Unable to initialize: " + e.getMessage());
            show(mayday);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Something unexpected happened", e);
            System.exit(1); // Swing GUI may be locked up so let's kill it
        }
    }
}
