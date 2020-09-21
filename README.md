# icalibrate
Ion chamber calibration desktop application for Jefferson Lab.  This software is developed with Java Swing.

![Screenshot](https://raw.githubusercontent.com/JeffersonLab/icalibrate/master/doc/Screenshot.png)

---
 - [Build](https://github.com/JeffersonLab/icalibrate#build)
 - [Configure](https://github.com/JeffersonLab/icalibrate#configure)
 - [Run](https://github.com/JeffersonLab/icalibrate#run)
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

Each of Hall _A_, _C_, and _D_ have one of these properties (Hall A shown):
| Property |Description |
|---|---|
| HALLA_EPICS_NAME_CSV | Comma separated values of EPICS PV base name for Ion Chambers; only used if NAMES_FROM_CED is false |
| HALLA_FRIENDLY_NAME_CSV | Comma separated values of Human Readable names for Ion Chambers; only used if NAMES_FROM_CED is false |
| HALLA_CED_NAME_CSV | Comma separated values of CED names for Ion Chambers; only used if NAMES_FROM_CED is false |
| HALLA_DOSE_READ_PV_SUFFIX | PV suffix for dose readback |
| HALLA_DOSE_SETPOINT_READ_PV_SUFFIX | PV suffix for dose read setpoint |
| HALLA_DOSE_SETPOINT_WRITE_PV_SUFFIX | PV suffix for dose write setpoint |
| HALLA_FRIENDLY_NAME_PV_SUFFIX | PV suffix for human readable ion chamber name |
| HALLA_TARGET_PV | Target name PV |
## Run
```
gradlew run
```
