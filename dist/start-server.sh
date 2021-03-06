#!/bin/sh
DBDIR=${DBDIR-$HOME/pjs/LibraryManager/PJS-DB}
WARFILE=${WARFILE-$HOME/pjs/LibraryManager/system/LibraryManager.war}
JETTY_RUNNER=${JETTY_RUNNER-$(dirname $0)/jetty-runner-8.1.8.v20121106.jar}

if /usr/bin/lsof -i:8080 > /dev/null 2>&1; then
    /usr/bin/xmessage "Server already running...."
    exit 1
fi

java -server -Xmx512M -Dderby.system.home=${DBDIR} -jar ${JETTY_RUNNER} ${WARFILE} 

#-Dkakasi.kanwaDictionary=${APPDIR}/kanwadict


