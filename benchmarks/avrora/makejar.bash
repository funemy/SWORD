#!/bin/bash

if [ "$1" = "" ]; then
    RELEASE=`java avrora.Version`
    if [ ! "$?" = 0 ]; then
        echo "  -> Error: could not get Avrora version."
        exit 1
    fi

else
    RELEASE=$1
fi

cd bin

JARFILE=avrora-$RELEASE.jar

jar cmf MANIFEST.MF ../jars/$JARFILE avrora cck
if [ ! "$?" = 0 ]; then
    echo "  -> Error: could not build jar file $JARFILE."
    exit 1
fi

echo $JARFILE
