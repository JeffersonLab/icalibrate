# icalibrate
Ion chamber calibration desktop application for Jefferson Lab.  This software is developed with Java Swing.

![Screenshot](https://raw.githubusercontent.com/JeffersonLab/icalibrate/master/doc/Screenshot.png)

---
 - [Build](https://github.com/JeffersonLab/icalibrate#build)
 - [Configure](https://github.com/JeffersonLab/icalibrate#configure)
 - [Run](https://github.com/JeffersonLab/icalibrate#run)
 - [See Also](https://github.com/JeffersonLab/icalibrate#see-also)
---

## Build
```
git clone https://github.com/JeffersonLab/icalibrate.git
cd icalibrate
gradlew build
```

## Configure
```
config/icalibrate.properties
```
| Property | Desription | Default |
|---|---|---|
| NAMES_FROM_CED | Whether Ion Chamber Names should be queryed from the CED, else properties of the form _HALLA_EPICS_NAME_CSV_ are used (Hall C and D too) | true |
| WRITE_ALLOWED | Whether the application can write to EPICS, else calibrations are dry-run simulations | true |
| LOGBOOK_CSV | Comma separated values of Jefferson Lab logbook names to write log entries to; set to TLOG for testing | ELOG |
| MASTER_FSD_VOLTAGE_PV | EPICS PV name to monitor for FSD trips | ISD0I011G |
| DEFAULT_HCD_FILE_DIR | Default location for file chooser when opening and savings dataset files | /usr/opsuser/mccops/IonChambers |

Each of Hall _A_, _C_, and _D_ have one of these properties (Hall A shown):

| Property |Description |
|---|---|
| HALLA_CURRENT_READ_PV | PV to determine current |
| HALLA_PASS_READ_PV | PV to determine pass |
| HALLA_DOSE_READ_PV_SUFFIX | PV suffix for dose readback |
| HALLA_DOSE_SETPOINT_READ_PV_SUFFIX | PV suffix for dose read setpoint |
| HALLA_DOSE_SETPOINT_WRITE_PV_SUFFIX | PV suffix for dose write setpoint |
| HALLA_FRIENDLY_NAME_PV_SUFFIX | PV suffix for human readable ion chamber name |
| HALLA_TARGET_PV | Target name PV |
| HALLA_MAX_CURRENT | Max hall current |
| HALLA_CURRENT_UNITS | Units for hall current |
| HALLA_MAX_MARGIN | Max hall margin |
| HALLA_NEGATIVE_MARGIN_CSV | Comma separated values of target names which default to a negative margin |
| HALLA_EPICS_NAME_CSV | Comma separated values of EPICS PV base name for Ion Chambers; only used if NAMES_FROM_CED is false |
| HALLA_FRIENDLY_NAME_CSV | Comma separated values of Human Readable names for Ion Chambers; only used if NAMES_FROM_CED is false |
| HALLA_CED_NAME_CSV | Comma separated values of CED names for Ion Chambers; only used if NAMES_FROM_CED is false |
## Run
```
gradlew run
```

**Note:** The _assembleDist_ Gradle target will build tar and zip files that can be distributed, and these distributions include start scripts to launch the application.

**Note:** To enable debug logging you can configure the JVM to use the logging properties file:  
``-Djava.util.logging.config.file=config/debug-logging.properties``

## See Also
   - [javadocs](https://jeffersonlab.github.io/icalibrate/)
   - [jlog](https://github.com/JeffersonLab/jlog)
