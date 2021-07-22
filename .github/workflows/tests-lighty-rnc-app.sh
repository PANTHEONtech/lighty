#!/bin/bash 
#
# Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at https://www.eclipse.org/legal/epl-v10.html
#

POD_SIMULATOR_IPS=$(kubectl get pods -l app.kubernetes.io/name=lighty-rnc-simulator -o custom-columns=":status.podIP" | xargs)
POD_CONTROLLER_IPS=$(kubectl get pods -l app.kubernetes.io/name=lighty-rnc-app-helm -o custom-columns=":status.podIP" | xargs)
MINIKUBE_IP=$(minikube ip)
HTTP_STATUS_CODES=("200","201","202","204")
declare -a test_results

echo POD_CONTROLLER_IPS
echo POD_SIMULATOR_IPS
echo MINIKUBE_IP

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
  if [[ "$1" =~ .*"connected".* ]]
  then
    echo "connected"
  else
    echo "not_connected"
  fi
}

# List pods
minikube kubectl -- get pods

# List Services
minikube kubectl -- get services

# Logs pods
pod_names=$(minikube kubectl -- get pods --no-headers -o custom-columns=":metadata.name")
for pod_name in $pod_names; \
do \
  minikube kubectl -- logs $pod_name \
;done

# Cluster state (:8558/cluster/members)
for pod_controller_ip in $POD_CONTROLLER_IPS; \
do \
  assertHttpStatusCode $(curl -o /dev/null -s -w "%{http_code} GET %{url_effective}\n"  --user admin:admin -H "Content-Type: application/json" --insecure http://$pod_controller_ip:8558/cluster/members) \
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
sleep 1

# add node into topology
for pod_simulator_ip in $POD_SIMULATOR_IPS; \
do \
  assertHttpStatusCode $(curl -X PUT -o /dev/null -s -w "%{http_code} PUT %{url_effective}\n" http://"$MINIKUBE_IP":30888/restconf/data/network-topology:network-topology/topology=topology-netconf/node=node-"${pod_simulator_ip//.}" \
  -H 'Content-Type: application/json' \
  -d '{
      "netconf-topology:node" :[
    	{
	      "node-id": "node-'${pod_simulator_ip//.}'",
	      "host": "'"$pod_simulator_ip"'",
        "port": 17830,
	      "username": "admin",
	      "password": "admin",
	      "tcp-only": false,
	      "keepalive-delay": 0
      }
    ]
  }')
done;
sleep 1

echo "Check if netconf-simulator is connected"
connection_status="not-connected"
for i in {1..10} ; do
  for pod_simulator_ip in $POD_SIMULATOR_IPS; \
  do \
  connection_status=$(assertNodeConnected $(curl -X GET -s \
  'http://'"$MINIKUBE_IP"':30888/restconf/data/network-topology:network-topology/topology=topology-netconf/node='node-"${pod_simulator_ip//.}"'/netconf-node-topology:connection-status'))
  done;
  sleep 1
  if [[ $connection_status == "connected" ]]
  then
    echo -e "Test passed\n"
    break;
  else
    echo -e "Connection status: $connection_status\n"
  fi
done

for pod_simulator_ip in $POD_SIMULATOR_IPS; \
do \
assertHttpStatusCode $(curl -X PUT -o /dev/null -s -w "%{http_code} PUT %{url_effective}\n" \
  http://"$MINIKUBE_IP":30888/restconf/data/network-topology:network-topology/topology=topology-netconf/node=node-"${pod_simulator_ip//.}"/yang-ext:mount/network-topology:network-topology \
  -H 'Content-Type: application/json' \
  -d '{
    "network-topology:network-topology": {
        "topology": [
            {
                "topology-id": "new-topology"
            }
        ]
    }
}')
done;
sleep 1

for pod_simulator_ip in $POD_SIMULATOR_IPS; \
do \
assertHttpStatusCode $(curl -X GET -o /dev/null -s -w "%{http_code} GET %{url_effective}\n" \
  http://"$MINIKUBE_IP":30888/restconf/data/network-topology:network-topology/topology=topology-netconf/node=node-"${pod_simulator_ip//.}"/yang-ext:mount/network-topology:network-topology/topology=new-topology \
  -H 'Content-Type: application/json')
done;
sleep 1

for pod_simulator_ip in $POD_SIMULATOR_IPS; \
do \
assertHttpStatusCode $(curl -X DELETE -o /dev/null -s -w "%{http_code} DELETE %{url_effective}\n" \
  http://"$MINIKUBE_IP":30888/restconf/data/network-topology:network-topology/topology=topology-netconf/node=node-"${pod_simulator_ip//.}"/yang-ext:mount/network-topology:network-topology/topology=new-topology)
done;
sleep 1

for pod_simulator_ip in $POD_SIMULATOR_IPS; \
do \
assertHttpStatusCode $(curl -X DELETE -o /dev/null -s -w "%{http_code} DELETE %{url_effective}\n" \
http://"$MINIKUBE_IP":30888/restconf/data/network-topology:network-topology/topology=topology-netconf/node=node-"${pod_simulator_ip//.}")
done;

# Check if some test failed
if [[ "${test_results[@]}" =~ 1 ]]
then
  exit 1;
fi
