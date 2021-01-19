#!/bin/sh

echo "[1/3] Exporting Docker image to .tar ..."
docker save --output="./lighty-image:latest.tar" lighty-rnc:latest

echo "[2/3] Uploading exported .tar to microk8s ..."
microk8s ctr --namespace k8s.io image import lighty-image\:latest.tar

echo "[3/3] Removing exported .tar ..."
rm lighty-image:latest.tar

echo "[done]"
