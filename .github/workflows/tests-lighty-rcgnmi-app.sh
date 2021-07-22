#!/bin/bash 
#
# Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at https://www.eclipse.org/legal/epl-v10.html
#

POD_SIMULATOR_IPS=$(kubectl get pods -l app.kubernetes.io/name=lighty-rcgnmi-simulator -o custom-columns=":status.podIP" | xargs)
POD_CONTROLLER_IPS=$(kubectl get pods -l app.kubernetes.io/name=lighty-rcgnmi-app-helm -o custom-columns=":status.podIP" | xargs)
MINIKUBE_IP=$(minikube ip)
HTTP_STATUS_CODES=("200","201","202","204")
declare -a test_results

assertHttpStatusCode() {
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

# Cluster state (:8558/cluster/members)
for pod_controller_ip in $POD_CONTROLLER_IPS; \
do \
  assertHttpStatusCode $(curl -o /dev/null -s -w "%{http_code} GET %{url_effective}\n" --user admin:admin -H "Content-Type: application/json" --insecure http://$pod_controller_ip:8558/cluster/members) \
;done
sleep 1

# Pods healthcheck (:8888/restconf/operations)
for pod_controller_ip in $POD_CONTROLLER_IPS; \
do \
  assertHttpStatusCode $(curl -o /dev/null -s -w "%{http_code} GET %{url_effective}\n" --user admin:admin -H "Content-Type: application/json" --insecure http://$pod_controller_ip:8888/restconf/operations) \
;done
sleep 1

# Service healthcheck (:30888/restconf/operations)
assertHttpStatusCode $(curl -o /dev/null -s -w "%{http_code} GET %{url_effective}\n" --user admin:admin -H "Content-Type: application/json" --insecure http://$MINIKUBE_IP:30888/restconf/operations)

# add gNMI node into gNMI topology
for pod_simulator_ip in $POD_SIMULATOR_IPS; \
do \
assertHttpStatusCode $(curl -X PUT -o /dev/null -s -w "%{http_code} PUT %{url_effective}\n" \
  http://"$MINIKUBE_IP":30888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-"${pod_simulator_ip//.}" \
  -H 'Content-Type: application/json' \
  -d '{
    "node": [
        {
            "node-id": "'node-"${pod_simulator_ip//.}"'",
            "connection-parameters": {
                "host": "'"$pod_simulator_ip"'",
                "port": 3333,
                "connection-type": "INSECURE",
                "credentials": {
                    "username": "Admin",
                    "password": "Admin"
                }
            }
        }
    ]
}')
done;
sleep 1

echo "Check if gnmi-simulator is connected"
connection_status="FAILURE"
for i in {1..10} ; do
  for pod_simulator_ip in $POD_SIMULATOR_IPS; \
  do \
  connection_status=$(assertNodeConnected $(curl -X GET -s \
  'http://'"$MINIKUBE_IP"':30888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node='node-"${pod_simulator_ip//.}"'/gnmi-topology:node-state/node-status'))
  done;
  sleep 1
  if [[ $connection_status == "READY" ]]
  then
    echo -e "Test passed\n"
    break;
  else
    echo -e "Connection status: $connection_status\n"
  fi
done

for pod_simulator_ip in $POD_SIMULATOR_IPS; \
do \
assertHttpStatusCode $(curl -X GET -o /dev/null -s -w "%{http_code} GET %{url_effective}\n" \
  'http://'"$MINIKUBE_IP"':30888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node='node-"${pod_simulator_ip//.}"'?content=config')
done;
sleep 1

for pod_simulator_ip in $POD_SIMULATOR_IPS; \
do \
assertHttpStatusCode $(curl -X PUT -o /dev/null -s -w "%{http_code} PUT %{url_effective}\n" \
  http://"$MINIKUBE_IP":30888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-"${pod_simulator_ip//.}"/yang-ext:mount/openconfig-system:system/aaa/authentication \
  -H 'Content-Type: application/json' \
  -d '{
    "openconfig-system:authentication": {
        "config": {
            "authentication-method": [
                "openconfig-aaa-types:TACACS_ALL"
            ]
        },
        "state": {
            "authentication-method": [
                "openconfig-aaa-types:RADIUS_ALL"
            ]
        },
        "admin-user": {
            "config": {
                "admin-password": "password"
            }
        }
    }
}')
done;
sleep 1

for pod_simulator_ip in $POD_SIMULATOR_IPS; \
do \
assertHttpStatusCode $(curl -X GET -o /dev/null -s -w "%{http_code} GET %{url_effective}\n" \
  'http://'"$MINIKUBE_IP"':30888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node='node-"${pod_simulator_ip//.}"'/yang-ext:mount/openconfig-system:system/aaa/authentication?content=nonconfig' \
  -H 'cache-control: no-cache')
done;
sleep 1

for pod_simulator_ip in $POD_SIMULATOR_IPS; \
do \
assertHttpStatusCode $(curl -X PATCH -o /dev/null -s -w "%{http_code} PATCH %{url_effective}\n" \
  http://"$MINIKUBE_IP":30888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-"${pod_simulator_ip//.}"/yang-ext:mount/openconfig-system:system/aaa/authentication/config \
  -H 'Content-Type: application/json' \
  -d '{
    "openconfig-system:config": {
        "authentication-method": [
            "openconfig-aaa-types:RADIUS_ALL"
        ]
    }
}')
done;
sleep 1

for pod_simulator_ip in $POD_SIMULATOR_IPS; \
do \
assertHttpStatusCode $(curl -X DELETE -o /dev/null -s -w "%{http_code} DELETE %{url_effective}\n" \
  http://"$MINIKUBE_IP":30888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-"${pod_simulator_ip//.}"/yang-ext:mount/openconfig-system:system/aaa/authentication/config)
done;
sleep 1

for pod_simulator_ip in $POD_SIMULATOR_IPS; \
do \
assertHttpStatusCode $(curl -X DELETE -o /dev/null -s -w "%{http_code} DELETE %{url_effective}\n" \
http://"$MINIKUBE_IP":30888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-"${pod_simulator_ip//.}")
done;

# Check if some test failed
if [[ "${test_results[@]}" =~ 1 ]]
then
  exit 1;
fi
