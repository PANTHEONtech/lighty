#!/bin/bash
#
# Copyright (c) 2022 PANTHEON.tech s.r.o. All Rights Reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at https://www.eclipse.org/legal/epl-v10.html
#

#
# --- GH Action test to verify RNC Cluster ---
# This test verifies if deployed RNC Cluster application is successfully running and app data-store is shared
# between clusters. After successfully verified, RNC APP deployment is scaled up to 5 pods. Verify if all 5 clusters
# are running correctly. Then is tested scaled down deployment to 3 clusters and also verify state.
# --------------------------------------------

CONTROLLER_PORT=8888
SIMULATOR_PORT=17830
HTTP_STATUS_CODES=("200" "201" "202" "204")
declare -a test_results

# Start lighty-netconf-simulator in minikube network
docker build -t lighty-netconf-simulator:latest ${GITHUB_WORKSPACE}/.github/workflows/lighty-rnc-app/simulator
kubectl delete pod netconf-simulator --ignore-not-found
kubectl apply -f ${GITHUB_WORKSPACE}/.github/workflows/lighty-rnc-app/simulator/netconf-simulator.yaml

# Wait until pod is ready
echo "Waiting for netconf-simulator pod to be ready..."
kubectl wait --for=condition=Ready pod/netconf-simulator --timeout=120s
echo "lighty-netconf-simulator is started.."

# Get netconf-simulator container's IP
SIMULATOR_IP=$(kubectl get pod netconf-simulator -o jsonpath="{.status.podIP}")

# check if simulator has opened port
for i in {1..20} ; do
    nc -z "$SIMULATOR_IP" "$SIMULATOR_PORT"
    if [ $? ]
    then
      echo "Simulator is successfully running"
      break;
    fi
    echo "counter: $i"
    sleep 1
done

isAllPodsReady() {
  pods_status=$(kubectl get pods -l app.kubernetes.io/name=lighty-rnc-app-helm -o custom-columns=":status.containerStatuses[*].ready")
  for podReady in $pods_status;
  do
    if [[ $podReady =~ .*"true".* ]]
    then \
      echo "Pod is ready [$podReady]"
    else
      echo "Pod is not ready [$podReady]"
      return 1
    fi
  done
  return 0
}

waitUntilPodsAreReady() {
  for i in {1..25} ;
    do
      if isAllPodsReady;
      then
        echo "PODS are ready"
        kubectl get pods -l app.kubernetes.io/name=lighty-rnc-app-helm -o custom-columns=":status.containerStatuses[*].ready" | xargs
        break;
      fi
      echo "Pods are not ready, counter: $i"
      sleep 5
  done
  if ! isAllPodsReady
  then
    echo "PODS are not ready in required time."
    stopTest
  fi
}

waitUntilPodsAreReady

# List pods
minikube kubectl -- get pods
# List Services
minikube kubectl -- get services
read -ra KUB_NAMES -d '' <<<"$(kubectl get pods --no-headers -o custom-columns=":metadata.name")"
POD_CONTROLLER_IPS=$(kubectl get pods -l app.kubernetes.io/name=lighty-rnc-app-helm -o custom-columns=":status.podIP" | xargs)

CTRL0_IP=$(kubectl get pod "${KUB_NAMES[0]}" -o custom-columns=":status.podIP" | xargs)
CTRL1_IP=$(kubectl get pod "${KUB_NAMES[1]}" -o custom-columns=":status.podIP" | xargs)
CTRL2_IP=$(kubectl get pod "${KUB_NAMES[2]}" -o custom-columns=":status.podIP" | xargs)
CTRL0_NAME=${KUB_NAMES[0]}
CTRL1_NAME=${KUB_NAMES[1]}
CTRL2_NAME=${KUB_NAMES[2]}

echo "Controller0 IPS set to: $CTRL0_IP running in pod name: $CTRL0_NAME"
echo "Controller1 IPS set to: $CTRL1_IP running in pod name: $CTRL1_NAME"
echo "Controller2 IPS set to: $CTRL2_IP running in pod name: $CTRL2_NAME"

printLine() {
  printf '%.0s-' {1..100}; echo ""
}

assertHttpStatusCode() {
  read -ra arr -d '' <<<"$1"
  printLine
  if [[ ${HTTP_STATUS_CODES[*]} =~ ${arr[0]} ]]
  then
    echo -e "HTTP request methods: ${arr[1]}\nURL: ${arr[2]}\nStatus Code: ${arr[0]}\nTest passed\n"
    test_results+=(0)
  else
    echo -e "HTTP request methods: ${arr[1]}\nURL: ${arr[2]}\nStatus Code: ${arr[0]}\nTest failed\n"
    test_results+=(1)
  fi
}

assertNodeConnected() {
  if [[ $1 =~ .*"connected".* ]]
  then
    echo "connected"
  else
    echo "$1"
  fi
}

assertPodsTopologyResponse() {
  local previousResponse=""
  for pod_controller_ip in $POD_CONTROLLER_IPS;
  do
    TOPOLOGY_RESPONSE=$(curl --request GET \
      "http://$pod_controller_ip:$CONTROLLER_PORT/restconf/data/network-topology:network-topology")
      if [[ -z "$TOPOLOGY_RESPONSE" ]]
      then
        echo "Empty response from pod ip: $pod_controller_ip"
        test_results+=(1)
        continue
      elif [[ -z "$previousResponse" ]]
      then
        # First request
        echo "first response $TOPOLOGY_RESPONSE"
        previousResponse=$TOPOLOGY_RESPONSE
        continue
      elif [[ "$previousResponse" != "$TOPOLOGY_RESPONSE" ]]
      then
        echo "Previous response doesn't match: [ $previousResponse ] with current response : [ $TOPOLOGY_RESPONSE ]"
        test_results+=(1)
        continue
      fi
      echo "Success compare for IP  $pod_controller_ip"
  done
}

validateTestStatus() {
  # Check if some test failed
  if [[ ${test_results[*]} =~ 1 ]]
  then
    docker stop netconf-simulator
    printf "\n------- Show pods -------"
    # List pods
    minikube kubectl -- get pods

    printf "\n------- Show Logs for every pod -------"
    pod_names=$(minikube kubectl -- get pods --no-headers -o custom-columns=":metadata.name")
    for pod_name in $pod_names; \
    do \
      minikube kubectl -- logs "$pod_name" \
    ;done

    printf "\n------- Describe every pod -------"
    for pod_name in $pod_names; \
    do \
      kubectl describe pod "$pod_name" \
    ;done
    exit 1;
  fi
}

printLine
echo "-- Lighty-rcgnmi-app curl tests --"

# Cluster state (:8558/cluster/members)
for pod_controller_ip in $POD_CONTROLLER_IPS; \
do \
  assertHttpStatusCode "$(curl -o /dev/null -s -w "%{http_code} GET %{url_effective}\n" \
   -H "Content-Type: application/json" \
   "http://$pod_controller_ip:8558/cluster/members")" \
;done
validateTestStatus

# Pods health check (:8888/restconf/operations)
for pod_controller_ip in $POD_CONTROLLER_IPS; \
do \
  assertHttpStatusCode "$(curl -o /dev/null -s -w "%{http_code} GET %{url_effective}\n" \
   -H "Content-Type: application/json" \
   "http://$pod_controller_ip:$CONTROLLER_PORT/restconf/operations")" \
;done

# Add node into topology
  assertHttpStatusCode "$(curl -X PUT -o /dev/null -s -w "%{http_code} PUT %{url_effective}\n" \
  "http://$CTRL0_IP:$CONTROLLER_PORT/restconf/data/network-topology:network-topology/topology=topology-netconf/node=node-${SIMULATOR_IP//.}" \
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
  }')"

printLine
echo "Check if netconf-simulator is connected"
connection_status="not-connected"
for i in {1..20} ; do
  connection_status=$(assertNodeConnected "$(curl -X GET -s \
  "http://$CTRL0_IP:$CONTROLLER_PORT/restconf/data/network-topology:network-topology/topology=topology-netconf/node=node-${SIMULATOR_IP//.}/netconf-node-topology:connection-status")")
  echo -e "Connection status: $connection_status"
  if [[ $connection_status == "connected" ]]
  then
    echo -e "Test passed\n"
    break;
  fi
  sleep 1
done

for pod_controller_ip in $POD_CONTROLLER_IPS; \
do \
  assertHttpStatusCode "$(curl -o /dev/null -s -w "%{http_code} GET %{url_effective}\n" \
    -H "Content-Type: application/json" \
     "http://$pod_controller_ip:$CONTROLLER_PORT/restconf/data/network-topology:network-topology")" \
;done

# Assert if topology response is equals for every cluster
assertPodsTopologyResponse
# Validate state of previous tests. If they fails stop test.
validateTestStatus

echo "Test resize deployment to 5 clusters"
echo "Show deployment"
minikube kubectl get deployments

echo "Scale replicas to 5"
kubectl scale deployments/lighty-rnc-app-lighty-rnc-app-helm --replicas=5
waitUntilPodsAreReady

echo "Show pods"
# List pods
minikube kubectl -- get pods

POD_CONTROLLER_IPS=$(kubectl get pods -l app.kubernetes.io/name=lighty-rnc-app-helm -o custom-columns=":status.podIP" | xargs)

## Assert if topology response is equals for every cluster after resize to 5 pods
assertPodsTopologyResponse
validateTestStatus

echo "Test resize deployment back to 3 clusters"
echo "Scale replicas to 3"
kubectl scale deployments/lighty-rnc-app-lighty-rnc-app-helm --replicas=3
# Wait until pods are terminated
sleep 35

echo "Show pods"
# List pods
minikube kubectl -- get pods

POD_CONTROLLER_IPS=$(kubectl get pods -l app.kubernetes.io/name=lighty-rnc-app-helm -o custom-columns=":status.podIP" | xargs)

## Assert if topology response is equals for every cluster after resize to 3 pods
assertPodsTopologyResponse
validateTestStatus

## Remove device
assertHttpStatusCode "$(curl -X DELETE -o /dev/null -s -w "%{http_code} DELETE %{url_effective}\n" \
 "http://$CTRL0_IP:$CONTROLLER_PORT/restconf/data/network-topology:network-topology/topology=topology-netconf/node=node-${SIMULATOR_IP//.}")"
# Wait until clusters are updated
sleep 15

assertPodsTopologyResponse
validateTestStatus
kubectl delete pod netconf-simulator --ignore-not-found
