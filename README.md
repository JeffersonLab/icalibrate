# icalibrate
Ion chamber calibration desktop application for Jefferson Lab.  This software is developed with Java Swing.

![Screenshot](https://raw.githubusercontent.com/JeffersonLab/icalibrate/master/doc/Screenshot.png)

---
 - [Build](https://github.com/slominskir/icalibrate)
 - [Run](https://github.com/slominskir/icalibrate)
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
| NAMES_FROM_CED | Whether Ion Chamber Names should be queryed from the CED | true |
| WRITE_ALLOWED | Whether the application can write to EPICS, else calibrations are dry-run simulations | true |
| LOGBOOK_CSV | Comma separated values of Jefferson Lab logbook names to write log entries to; set to TLOG for testing | ELOG |
## Run
```
gradlew run
```
