#!/bin/bash
cd target
APP=""
for line in $(find . -maxdepth 1 -type f -iname "lighty*-bin.zip");
do
    APP=${line:2:-8}
    break
done
if [ -z "$APP" ]; then
    echo "Error: lighty-cluster-app JAR is missing in target directory. Rebuild the project using (mvn clean install) and try again."
    exit
fi
if [ ! -d  $APP ]; then
    echo "Unzipping .bin.zip"
    unzip $"$APP-bin.zip"
fi

if [ ! -d  "$APP-01" ]; then
    echo "Cloning $APP-01"
    cp -a $APP "$APP-01"
fi
if [ ! -d  "$APP-02" ]; then
    echo "Cloning $APP-02"
    cp -a $APP "$APP-02"
fi
if [ ! -d  "$APP-03" ]; then
    echo "Cloning $APP-03"
    cp -a $APP "$APP-03"
fi


echo "Running Lighty instances"
x-terminal-emulator -hold -geometry 240x60+10+10 -T "$APP-01" -e "cd $APP-01&&./start-controller-node-01.sh" &
x-terminal-emulator -hold -geometry 240x60+50+50 -T "$APP-02" -e "cd $APP-02&&./start-controller-node-02.sh" &
x-terminal-emulator -hold -geometry 240x60+90+90 -T "$APP-03" -e "cd $APP-03&&./start-controller-node-03.sh" &