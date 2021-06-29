#!/bin/bash

IFS=$(printf '\n.'); IFS=${IFS%.}

for LINE in $(microk8s kubectl get pods -o wide | grep 'lighty-rcgnmi-app'); do
        echo ${LINE}
        POD=`echo "${LINE}" | awk '{ print $1 }'`
        PODIP=`echo "${LINE}" | awk '{ print $6 }'`
        echo "Saving logs from POD ${POD}"
        microk8s kubectl logs "${POD}" > "${POD}-${PODIP}.log"
done;
