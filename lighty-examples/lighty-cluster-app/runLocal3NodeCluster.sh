#!/bin/bash
cd target
if [ ! -d  lighty-cluster-app-10.1.1-SNAPSHOT ]; then
    echo "Unzipping .bin.zip"
    unzip lighty-cluster-app-10.1.1-SNAPSHOT-bin.zip
fi
if [ ! -d  lighty-cluster-app-10.1.1-SNAPSHOT-01 ]; then
    echo "Cloning 01"
    cp -a lighty-cluster-app-10.1.1-SNAPSHOT lighty-cluster-app-10.1.1-SNAPSHOT-01
fi
if [ ! -d  lighty-cluster-app-10.1.1-SNAPSHOT-02 ]; then
    echo "Cloning 02"
    cp -a lighty-cluster-app-10.1.1-SNAPSHOT lighty-cluster-app-10.1.1-SNAPSHOT-02
fi
if [ ! -d  lighty-cluster-app-10.1.1-SNAPSHOT-03 ]; then
    echo "Cloning 03"
    cp -a lighty-cluster-app-10.1.1-SNAPSHOT lighty-cluster-app-10.1.1-SNAPSHOT-03
fi


echo "Running Lighty instances"
x-terminal-emulator -hold -geometry 240x60+10+10 -T "lighty-cluster-app-10.1.1-SNAPSHOT-01" -e "cd lighty-cluster-app-10.1.1-SNAPSHOT-01&&./start-controller-node-01.sh" &
x-terminal-emulator -hold -geometry 240x60+50+50 -T "lighty-cluster-app-10.1.1-SNAPSHOT-02" -e "cd lighty-cluster-app-10.1.1-SNAPSHOT-02&&./start-controller-node-02.sh" &
x-terminal-emulator -hold -geometry 240x60+90+90 -T "lighty-cluster-app-10.1.1-SNAPSHOT-03" -e "cd lighty-cluster-app-10.1.1-SNAPSHOT-03&&./start-controller-node-03.sh" &