#!/bin/sh

REQUIRED_VERSION=1.8

if ! type -p java > /dev/null; then
        echo "java command not found";
        exit;
fi

MYPATH="$(readlink -f "$0")"
MYDIR="${MYPATH%/*}"

# Note: the -Djava.security.egd flag may now be unnecessary, but once was needed
# because the Oracle database driver uses the /dev/random pseudo-device for 
# random numbers on Linux, and in our environment this device used to lack 
# enough entropy to prevent long blocking.  This means users would sometimes see
# waits of 1 - 30+ seconds waiting for the driver to initialize!  The rngd 
# daemon should now be running to feed /dev/random and avoid this issue, but 
# this flag indicates the the non-blocking, but less secure /dev/urandom should
# be used instead.

java -DCAJ_STRIP_HOSTNAME=CAJ_STRIP_HOSTNAME -Djava.util.logging.config.file=config/debug-logging.properties -Djava.security.egd=file:/dev/./urandom -jar $MYDIR/icalibrate.jar $@
