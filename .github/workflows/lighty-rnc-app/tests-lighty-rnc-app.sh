#!/bin/bash 
#
# Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at https://www.eclipse.org/legal/epl-v10.html
#

POD_CONTROLLER_IPS=$(kubectl get pods -l app.kubernetes.io/name=lighty-rnc-app-helm -o custom-columns=":status.podIP" | xargs)
POD_CONTROLLER_NAME=$(kubectl get pods -l app.kubernetes.io/name=lighty-rnc-app-helm -o custom-columns=:".metadata.name" | xargs)
CONTROLLER_PORT=30888
SIMULATOR_PORT=17830
MINIKUBE_IP=$(minikube ip)
HTTP_STATUS_CODES=("200","201","202","204")
declare -a test_results

# Start lighty-netconf-simulator in minikube network
docker build -t lighty-netconf-simulator:latest ${GITHUB_WORKSPACE}/.github/workflows/lighty-rnc-app/simulator
kubectl delete pod netconf-simulator --ignore-not-found
kubectl apply -f ${GITHUB_WORKSPACE}/.github/workflows/lighty-rnc-app/simulator/netconf-simulator.yaml

# Wait until pod is ready
echo "Waiting for netconf-simulator pod to be ready..."
kubectl wait --for=condition=Ready pod/netconf-simulator --timeout=120s

# Get simulator pod IP
SIMULATOR_IP=$(kubectl get pod netconf-simulator -o jsonpath="{.status.podIP}")

# List pods
kubectl get pods

# List Services
kubectl get Services

# Logs pods
for pod_name in $(kubectl get pods --no-headers -o custom-columns=":metadata.name"); do
  echo -e "\n--- Logs for $pod_name ---"
  kubectl logs "$pod_name"
done

# check if simulator has opened port
for i in {1..20} ; do
    nc -z $SIMULATOR_IP $SIMULATOR_PORT
    if [ $? ]
    then
      break;
    fi
    echo "counter: $i"
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
  if [[ "$1" =~ .*"connected".* ]]
  then
    echo "connected"
  else
    echo "not_connected"
  fi
}

printLine() {
  printf '%.0s-' {1..100}; echo ""
}

printLine
echo "-- Lighty-rcgnmi-app curl tests --"

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
assertHttpStatusCode $(curl -o /dev/null -s -w "%{http_code} GET %{url_effective}\n" --user admin:admin -H "Content-Type: application/json" --insecure http://$MINIKUBE_IP:$CONTROLLER_PORT/restconf/operations)
sleep 1

# add node into topology
  assertHttpStatusCode $(curl -X PUT -o /dev/null -s -w "%{http_code} PUT %{url_effective}\n" http://"$MINIKUBE_IP":$CONTROLLER_PORT/restconf/data/network-topology:network-topology/topology=topology-netconf/node=node-"${SIMULATOR_IP//.}" \
  -H 'Content-Type: application/json' \
  -d '{
      "netconf-topology:node" :[
    	{
	      "node-id": "node-'"${SIMULATOR_IP//.}"'",
	      "host": "'"$SIMULATOR_IP"'",
        "port": '"$SIMULATOR_PORT"',
        "login-password-unencrypted": {
	        "username": "admin",
	        "password": "admin"
	      },
	      "tcp-only": false,
	      "keepalive-delay": 0
      }
    ]
  }')
sleep 1

printLine
echo "Check if netconf-simulator is connected"
connection_status="not-connected"
for i in {1..20} ; do
  connection_status=$(assertNodeConnected $(curl -X GET -s \
  'http://'"$MINIKUBE_IP"':'"$CONTROLLER_PORT"'/restconf/data/network-topology:network-topology/topology=topology-netconf/node='node-"${SIMULATOR_IP//.}"'/netconf-node-topology:connection-status'))
  echo -e "Connection status: $connection_status"
  if [[ $connection_status == "connected" ]]
  then
    echo -e "Test passed\n"
    break;
  fi
  sleep 1
done

assertHttpStatusCode $(curl -X PUT -o /dev/null -s -w "%{http_code} PUT %{url_effective}\n" \
  http://"$MINIKUBE_IP":$CONTROLLER_PORT/restconf/data/network-topology:network-topology/topology=topology-netconf/node=node-"${SIMULATOR_IP//.}"/yang-ext:mount/network-topology:network-topology \
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
sleep 1

assertHttpStatusCode $(curl -X GET -o /dev/null -s -w "%{http_code} GET %{url_effective}\n" \
  http://"$MINIKUBE_IP":$CONTROLLER_PORT/restconf/data/network-topology:network-topology/topology=topology-netconf/node=node-"${SIMULATOR_IP//.}"/yang-ext:mount/network-topology:network-topology/topology=new-topology \
  -H 'Content-Type: application/json')
sleep 1

assertHttpStatusCode $(curl -X DELETE -o /dev/null -s -w "%{http_code} DELETE %{url_effective}\n" \
  http://"$MINIKUBE_IP":$CONTROLLER_PORT/restconf/data/network-topology:network-topology/topology=topology-netconf/node=node-"${SIMULATOR_IP//.}"/yang-ext:mount/network-topology:network-topology/topology=new-topology)
sleep 1

assertHttpStatusCode $(curl -X DELETE -o /dev/null -s -w "%{http_code} DELETE %{url_effective}\n" \
http://"$MINIKUBE_IP":$CONTROLLER_PORT/restconf/data/network-topology:network-topology/topology=topology-netconf/node=node-"${SIMULATOR_IP//.}")

# Check if some test failed
if [[ "${test_results[@]}" =~ 1 ]]
then
  docker stop netconf-simulator
  exit 1;
fi

kubectl delete pod netconf-simulator --ignore-not-found