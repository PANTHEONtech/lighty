#!/bin/sh

echo "[1/3] Exporting Docker image to .tar ..."
docker save --output="./lighty-rcgnmi:latest.tar" lighty-rcgnmi:latest

echo "[2/3] Uploading exported .tar to microk8s ..."
microk8s ctr --namespace k8s.io image import lighty-rcgnmi:latest.tar
#
#echo "[3/3] Removing exported .tar ..."
#rm lighty-rcgnmi:latest.tar
#
#echo "[done]"
