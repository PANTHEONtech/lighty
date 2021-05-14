#!/bin/bash

IFS=$'\n'
for LINE in $(microk8s kubectl get pods -o wide | grep 'lighty-helm'); do
        POD=`echo "${LINE}" | awk '{ print $1 }'`
        PODIP=`echo "${LINE}" | awk '{ print $6 }'`
        echo "Saving logs from POD ${POD}"
        microk8s kubectl logs "${POD}" > "${POD}-${PODIP}.log"
done;
