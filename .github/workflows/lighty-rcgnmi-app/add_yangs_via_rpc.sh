#!/bin/bash
#
# Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

MINIKUBE_IP=$(minikube ip)
YANG_PATHS="yangs"

printLine() {
  echo "";printf '%.0s-' {1..100}; echo ""
}

printLine
echo -e "-- Loading YANG models to gNMI lighty.io app --\n"

for yangFile in `ls -A1 $YANG_PATHS`
do \
  printLine
  modelName=$(head -n1 $YANG_PATHS/$yangFile | awk '{print $2}')
  revision=$(egrep -o "[0-9]{4}-[0-9]{2}-[0-9]{2}" $YANG_PATHS/$yangFile | head -n1)
  version=$(egrep -o "[0-9]{1}\.[0-9]{1}\.[0-9]{1}" $YANG_PATHS/$yangFile | head -n1)
  modelBody=$(sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' $YANG_PATHS/$yangFile)

  if [ -n $version ]
  then
    revision=$version
  fi

  curl -X POST -o /dev/null -s -w "Model: $modelName\nStatus Code: %{http_code}\nURL: %{url_effective}\n\r" \
  http://"$MINIKUBE_IP":30888/restconf/operations/gnmi-yang-storage:upload-yang-model \
  -H 'Content-Type: application/json' \
  -d '{
          "input": {
              "name": "'"$modelName"'",
              "version": "'"$revision"'",
              "body": "'"$modelBody"'"
          }
      }'
done

