#!/bin/bash

##########################################################################
#               N O T   F O R   P U B L I C   U S E
##########################################################################
#
#  This bash script is used by the Avrora developers to commit changes
#  to CVS and is not intended to be used by users. It checks multiple
#  correctness criteria, runs the tests, and increments the build number.
#
##########################################################################

if [ "$1" = "" ]; then
    echo "Usage: commit.bash <log message>"
    exit
fi

RED='[0;31m'
GREEN='[0;32m'
NORM='[0;00m'

JAVA_FILES=`find src -name '*.java'`
JJ_FILES=`find src -name '*.jj'`

MODULES='cck jintgen avrora'
ROOTPATH=`pwd`
JAVA_avrora='java'
JAVA_jintgen='java5'

removeOldVersions() {
	for m in $MODULES; do
	    rm -f /tmp/${m}Version.java
	done
}

restoreOldVersions() {
	for m in $MODULES; do
	    if `test -e /tmp/${m}Version.java`; then
		cp /tmp/${m}Version.java $ROOTPATH/src/$m/Version.java
	    fi
	done
}

report() {
    if [ "$3" = "" ]; then
	echo "  -> $1$2${NORM}"
    else
	echo "  -> $1$2${NORM}: $3"
    fi
}

reportError() {
    report "$RED" "$1" "$2"
}

reportSuccess() {
    report "$GREEN" "$1" "$2"
}

# routine to check for successful CVS commit conditions
checkSuccess() {

    if [ "$?" = 0 ]; then
	if [ ! "$1" = "" ]; then
	    reportSuccess "$1"
	fi
    else
	reportError "Commit error" "$2"
	$3
	cat /tmp/commit.reason
	# replace all old version files
	restoreOldVersions
	removeOldVersions
	exit 1
    fi
}

assembleCommitErrors() {
    cp /tmp/commit.log /tmp/commit.reason
}

assembleMissing() {
    echo '*** The following files are not in CVS: ***' > /tmp/commit.reason
    echo `grep cvs\ log:\ nothing\ known\ about /tmp/commit.new | awk '{ print $6 }'` >> /tmp/commit.reason
}

assembleCompileErrors() {
    cp /tmp/commit.log /tmp/commit.reason
}

assembleTestErrors() {
    cp /tmp/commit.log /tmp/commit.reason
}

assembleCheckinList() {
    echo '*** The following files are in CVS but missing here: ***'
    grep cvs\ diff:\ cannot\ find /tmp/commit.log | awk '{ print $5}' >> /tmp/commit.reason
}

echo > /tmp/commit.reason

#echo 'Checking for bash mode "avrora"'
#test "$BASH_MODE" = "avrora"
#checkSuccess 'OK' 'Please convert shell to avrora mode first.' 'echo'

echo 'Checking that all Java files are added to CVS...'
cvs log $JAVA_FILES &> /tmp/commit.new
checkSuccess 'All Java files here are in CVS.' 'There are Java files missing from CVS.' 'assembleMissing'

echo 'Checking that any changes need to be committed...'
cvs diff &> /tmp/commit.log
if `test "$?" = 0`; then
    echo " -> No changes to commit."
    exit 0
else
    test `grep cvs\ diff:\ cannot\ find /tmp/commit.log | wc -l` = 0
    checkSuccess 'No files are missing from CVS.' 'Files are missing here that are in CVS.' 'assembleCheckinList'
fi

echo "Removing all class files with \"make clean\""
make clean

for m in $MODULES; do
    echo "Checking module $m..."
    MODULE_FILES=`find src/$m -name '*.java'`

    cvs diff src/$m &> /tmp/commit.log
    if `test "$?" = 0`; then
	echo " -> No changes to commit."
    else

    VERSION_JAVA=src/$m/Version.java

    echo "> Incrementing build number..."
    test -e $VERSION_JAVA
    checkSuccess 'Version.java exists.' 'Version.java does not exist' 'echo'

    cp $VERSION_JAVA /tmp/${m}Version.java
    awk '{ if ( $1 == "public" && $4 == "int" && $5 == "commit" ) printf("    public static final int commit = %d;\n",($7+1)); else print }' /tmp/${m}Version.java > $VERSION_JAVA
    fi

    echo "> Making $m..."
    make $m
    checkSuccess 'Compiled successfully.' 'There were compilation errors building the project.' 'assembleCompileErrors'
done
   
TESTS='interpreter probes disassembler interrupts timers'
for t in $TESTS; do

    echo Running tests in test/$t...
    test -d test/$t
    checkSuccess "test/$t exists." "test/$t does not exist." 'cat > /tmp/commit.reason'

    cd test/$t
    java avrora.Main -action=test *.tst &> /tmp/commit.log
    checkSuccess 'All tests passed.' 'There were test case failures.' 'assembleTestErrors'
    cd $ROOTPATH
done

echo Making jar archive...
JARFILE=`./makejar.bash`
checkSuccess "$JARFILE created successfully." 'There were errors creating the JAR file.' 'assembleCommitErrors'

echo Attempting to commit to CVS...

# check that all Java files are added and nothing is missing, etc
cvs commit -m "$1" &> /tmp/commit.log
checkSuccess 'Commit completed successfully.' 'There were errors committing to CVS.' 'assembleCommitErrors'

cat /tmp/commit.log

removeOldVersions
#mv jars/$JARFILE /project/www/html/avrora/jars
