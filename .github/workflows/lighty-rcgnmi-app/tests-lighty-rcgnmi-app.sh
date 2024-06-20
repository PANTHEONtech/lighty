#!/bin/bash 
#
# Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at https://www.eclipse.org/legal/epl-v10.html
#

printLine() {
  printf '%.0s-' {1..100}; echo ""
}

POD_CONTROLLER_IPS=$(kubectl get pods -l app.kubernetes.io/name=lighty-rcgnmi-app-helm -o custom-columns=":status.podIP" | xargs)
POD_CONTROLLER_NAME=$(kubectl get pods -l app.kubernetes.io/name=lighty-rcgnmi-app-helm -o custom-columns=:".metadata.name" | xargs)
CONTROLLER_PORT=30888
SIMULATOR_PORT=3333
MINIKUBE_IP=$(minikube ip)
HTTP_STATUS_CODES=("200","201","202","204")
declare -a test_results

# List pods
minikube kubectl -- get pods

# Logs pods
pod_names=$(minikube kubectl -- get pods --no-headers -o custom-columns=":metadata.name")
for pod_name in $pod_names; \
do \
  minikube kubectl -- logs $pod_name \
;done

# List Services
minikube kubectl -- get services

# change directory to lighty-rcgnmi-app
cd ${GITHUB_WORKSPACE}/.github/workflows/lighty-rcgnmi-app/

printLine
echo -e "-- Downloading YANG models for test --\n"
./download_yangs.sh
echo -e "Downloaded YANG models:\n"
ls -1 yangs

#Run simulator for testing purpose
printLine
echo -e "-- Starting gNMI simulator device --\n"
java -jar ${GITHUB_WORKSPACE}/lighty-modules/lighty-gnmi/lighty-gnmi-device-simulator/target/lighty-gnmi-device-simulator-20.2.0-SNAPSHOT.jar -c ./simulator/example_config.json > /dev/null 2>&1 &

#Add yangs into controller through REST rpc
./add_yangs_via_rpc.sh

# check if simulator has opened port
for i in {1..5} ; do
    nc -z $MINIKUBE_IP $SIMULATOR_PORT
    if [ $? ]
    then
      break;
    fi
    sleep 1
done

assertHttpStatusCode() {
  printLine
  if [[ "${HTTP_STATUS_CODES[@]}" =~ "$1" ]]
  then
    echo -e "HTTP request methods: $2\nURL: $3\nStatus Code: $1\nTest passed\n"
    test_results+=(0)
  else
    echo -e "HTTP request methods: $2\nURL: $3\nStatus Code: $1\nTest failed\n"
    test_results+=(1)
  fi
}

assertNodeConnected() {
  if [[ "$1" =~ .*"READY".* ]]
  then
    echo "READY"
  else
    echo "FAILURE"
  fi
}

printLine
echo -e "-- Lighty-rcgnmi-app curl tests --\n"

# Cluster state (:8558/cluster/members)
for pod_controller_ip in $POD_CONTROLLER_IPS; \
do \
  assertHttpStatusCode $(curl -o /dev/null -s -w "%{http_code} GET %{url_effective}\n" --user admin:admin -H "Content-Type: application/json" --insecure http://$pod_controller_ip:8558/cluster/members) \
;done
sleep 1

#Pods healthcheck (:8888/restconf/operations)

for pod_controller_ip in $POD_CONTROLLER_IPS; \
do \
  assertHttpStatusCode $(curl -o /dev/null -s -w "%{http_code} GET %{url_effective}\n" --user admin:admin -H "Content-Type: application/json" --insecure http://$pod_controller_ip:8888/restconf/operations) \
;done
sleep 1

#Service healthcheck (:30888/restconf/operations)
assertHttpStatusCode $(curl -o /dev/null -s -w "%{http_code} GET %{url_effective}\n" --user admin:admin -H "Content-Type: application/json" --insecure http://$MINIKUBE_IP:$CONTROLLER_PORT/restconf/operations)

# add gNMI node into gNMI topology
assertHttpStatusCode $(curl -X PUT -o /dev/null -s -w "%{http_code} PUT %{url_effective}\n" \
  http://"$MINIKUBE_IP":$CONTROLLER_PORT/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-"${MINIKUBE_IP//.}" \
  -H 'Content-Type: application/json' \
  -d '{
    "node": [
        {
            "node-id": "'node-"${MINIKUBE_IP//.}"'",
            "connection-parameters": {
                "host": "'"$MINIKUBE_IP"'",
                "port": '"$SIMULATOR_PORT"',
                "connection-type": "INSECURE",
                "credentials": {
                    "username": "admin",
                    "password": "admin"
                }
            }
        }
    ]
}')
sleep 1

printLine
echo "Check if gnmi-simulator is connected"
connection_status="FAILURE"
for i in {1..10} ; do
  connection_status=$(assertNodeConnected $(curl -X GET -s \
  'http://'"$MINIKUBE_IP"':'"$CONTROLLER_PORT"'/restconf/data/network-topology:network-topology/topology=gnmi-topology/node='node-"${MINIKUBE_IP//.}"'/gnmi-topology:node-state/node-status'))
  echo -e "Connection status check $i: $connection_status"
  if [[ $connection_status == "READY" ]]
  then
    echo -e "Test passed"
    break;
  fi
  sleep 1
done
echo ""

assertHttpStatusCode $(curl -X GET -o /dev/null -s -w "%{http_code} GET %{url_effective}\n" \
  'http://'"$MINIKUBE_IP"':'"$CONTROLLER_PORT"'/restconf/data/network-topology:network-topology/topology=gnmi-topology/node='node-"${MINIKUBE_IP//.}"'?content=config')
sleep 1

assertHttpStatusCode $(curl -X PUT -o /dev/null -s -w "%{http_code} PUT %{url_effective}\n" \
  http://"$MINIKUBE_IP":$CONTROLLER_PORT/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-"${MINIKUBE_IP//.}"/yang-ext:mount/openconfig-system:system/aaa/authentication \
  -H 'Content-Type: application/json' \
  -d '{
    "openconfig-system:authentication": {
        "config": {
            "authentication-method": [
                "openconfig-aaa-types:TACACS_ALL"
            ]
        },
        "admin-user": {
            "config": {
                "admin-password": "password"
            }
        }
    }
}')
sleep 1

assertHttpStatusCode $(curl -X GET -o /dev/null -s -w "%{http_code} GET %{url_effective}\n" \
  'http://'"$MINIKUBE_IP"':'"$CONTROLLER_PORT"'/restconf/data/network-topology:network-topology/topology=gnmi-topology/node='node-"${MINIKUBE_IP//.}"'/yang-ext:mount/openconfig-system:system/aaa/authentication?content=nonconfig' \
  -H 'cache-control: no-cache')
sleep 1

assertHttpStatusCode $(curl -X PATCH -o /dev/null -s -w "%{http_code} PATCH %{url_effective}\n" \
  http://"$MINIKUBE_IP":$CONTROLLER_PORT/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-"${MINIKUBE_IP//.}"/yang-ext:mount/openconfig-system:system/aaa/authentication/config \
  -H 'Content-Type: application/json' \
  -d '{
    "openconfig-system:config": {
        "authentication-method": [
            "openconfig-aaa-types:RADIUS_ALL"
        ]
    }
}')
sleep 1

assertHttpStatusCode $(curl -X DELETE -o /dev/null -s -w "%{http_code} DELETE %{url_effective}\n" \
  http://"$MINIKUBE_IP":$CONTROLLER_PORT/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-"${MINIKUBE_IP//.}"/yang-ext:mount/openconfig-system:system/aaa/authentication/config)
sleep 1

assertHttpStatusCode $(curl -X DELETE -o /dev/null -s -w "%{http_code} DELETE %{url_effective}\n" \
http://"$MINIKUBE_IP":$CONTROLLER_PORT/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-"${MINIKUBE_IP//.}")

# Check if some test failed
if [[ "${test_results[@]}" =~ 1 ]]
then
  #kill last background process (gNMI simulator)
  kill $!
  exit 1;
fi

#kill last background process (gNMI simulator)
kill $!
