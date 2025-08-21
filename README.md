# icalibrate [![CI](https://github.com/JeffersonLab/icalibrate/actions/workflows/ci.yaml/badge.svg)](https://github.com/JeffersonLab/icalibrate/actions/workflows/ci.yaml)
Ion chamber calibration desktop application for Jefferson Lab.  This software is developed with Java Swing.

![Screenshot](https://raw.githubusercontent.com/JeffersonLab/icalibrate/master/doc/Screenshot.png)

---
 - [Overview](https://github.com/JeffersonLab/icalibrate#overview)
 - [Install](https://github.com/JeffersonLab/icalibrate#install)
 - [Configure](https://github.com/JeffersonLab/icalibrate#configure)
 - [Build](https://github.com/JeffersonLab/icalibrate#build)
 - [Develop](https://github.com/JeffersonLab/icalibrate#develop)
 - [Release](https://github.com/JeffersonLab/icalibrate#release)
 - [Deploy](https://github.com/JeffersonLab/icalibrate#deploy)  
 - [See Also](https://github.com/JeffersonLab/icalibrate#see-also)
---

## Overview
The iCalibrate application provides operators the ability to perform hall ion chamber calibration.   The app presents operators with a wizard dialog to guide them through the process of gathering dose rate measurements.  Based on options provided by operators the software will ramp the hall laser attenuator incrementally and measure dose rates at the various resulting currents.  The software will then use the gathered data to graph a scatter plot with a fit to allow quickly determining an appropriate trip setpoint.  The data can be saved in a Hall Calibration Dataset (HCD) file to be later recalled if necessary.  The ability to apply computed setpoints to the EPICS control system or alternatively to output a SNAP file for future use is also provided.

See [User Guide](https://github.com/JeffersonLab/icalibrate/raw/refs/heads/main/doc/iCalibrate%20User%20Guide.docx)

## Install
This application requires a Java 11+ JVM and standard library to run.

Download from [Releases](https://github.com/JeffersonLab/icalibrate/releases) or [build](https://github.com/JeffersonLab/icalibrate#build) the [distribution](https://github.com/JeffersonLab/icalibrate#release) yourself.

Launch with:

UNIX:
```
bin/icalibrate
```
Windows:
```
bin/icalibrate.bat
```

**Note:** To enable debug logging, run with CA writes disabled, and logbook set to _TLOG_ use the __testRun__ Gradle task

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

## Build
This project is built with [Java 21](https://adoptium.net/) (compiled to Java 11 bytecode), and uses the [Gradle 9](https://gradle.org/) build tool to automatically download dependencies and build the project from source:

```
git clone https://github.com/JeffersonLab/icalibrate.git
cd icalibrate
gradlew build
```

**Note**: If you do not already have Gradle installed, it will be installed automatically by the wrapper script included in the source

**Note for JLab On-Site Users**: Jefferson Lab has an intercepting [proxy](https://gist.github.com/slominskir/92c25a033db93a90184a5994e71d0b78)

## Develop
In order to iterate rapidly when making changes it's often useful to run the app directly on the local workstation, perhaps leveraging an IDE. In this scenario run the service dependencies with:
```
docker compose -f deps.yaml up
```

And run the app with test configuration with:
```
gradlew testRun
```

**Note**: The app can be run in a mode that does not require the CED (an on-site connection) using the `NAMES_FROM_CED` property, which is what the test properties sets.

**Note**: Javadocs can be generated with the command:
```
gradlew javadoc
```

**Note**: The graphical Java Swing forms were built using the [Apache Netbeans](https://netbeans.apache.org/) Matisse builder tool.  It's recommended that graphical component modifications be made using this tool, which modifies the XML `*.form` files.  The XML is used to dyanamically generate Java Swing code.

## Release
1. Bump the version number in the VERSION file and commit and push to GitHub (using [Semantic Versioning](https://semver.org/)).
1. The CD GitHub Action should run automatically invoking:
     - The Create release GitHub Action to tag the source and create release notes summarizing any pull requests. Edit the release notes to add any missing details. A distribution zip file artifact is attached to the release.

## Deploy
At Jefferson Lab the app and all of it's data is stored on the ops network fileystem at `/cs/opshome/IonChambers` and can be launched via JMenu using search keyword `icalibrate`.  Deploying a new version typically looks like (version 2.0.0 shown):

```
ssh root@opsfs
cd /tmp
wget https://github.com/JeffersonLab/icalibrate/releases/download/v2.0.0/icalibrate-2.0.0.zip
unzip icalibrate-2.0.0.zip
mv icalibrate-2.0.0 /cs/opshome/IonChambers/icalibrate/2.0.0
cd /cs/opshome/IonChambers
unlink pro
ln -s icalibrate/2.0.0 pro
```

## See Also
   - [phaser-client](https://github.com/JeffersonLab/phaser-client)
   - [jlog](https://github.com/JeffersonLab/jlog) 
