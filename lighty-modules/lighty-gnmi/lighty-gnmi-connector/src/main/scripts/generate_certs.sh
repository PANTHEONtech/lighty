#!/bin/sh

#
# Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at https://www.eclipse.org/legal/epl-v10.html
#

mkdir certs
cd certs

SUBJ="/C=NZ/ST=Test/L=Test/O=Test/OU=Test/CN=ca"

# Generate CA Private Key
openssl genrsa -out ca.key 4096

# Generate Req
openssl req -new -x509 -days 3650 -subj $SUBJ -key ca.key -out ca.crt

SUBJ="/C=NZ/ST=Test/L=Test/O=Test/OU=Test/CN=localhost"

# Generate Server Private Key
openssl genrsa -out server.key 4096

# Transform server private key to pkcs8 format (Required by lighy-gnmi-simulator)
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in server.key -out server-pkcs8.key

# Generate Req
openssl req -new -sha256 -out server.csr -key server.key -subj $SUBJ

# Generate x509 with signed CA
openssl x509 \
        -req \
        -in server.csr \
        -CA ca.crt \
        -CAkey ca.key \
        -CAcreateserial \
        -days 3650 \
        -sha256 \
        -extensions req_ext \
        -extfile ../server_cert_ext.cfg \
        -out server.crt

SUBJ="/C=NZ/ST=Test/L=Test/O=Test/OU=Test/CN=client.com"

# Generate Client Private Key
openssl genrsa -out client.key 4096

# Generate Req
openssl req -new -key client.key -out client.csr -subj $SUBJ

# Generate x509 with signed CA
openssl x509 \
        -req \
        -in client.csr \
        -CA ca.crt \
        -CAkey ca.key \
        -days 3650 \
        -sha256 \
        -extfile ../client_cert_ext.cfg \
        -out client.crt \
        -CAcreateserial

# Generate Client Private Key with passphrase
openssl genrsa -aes256 -passout file:../client_key_passphrase.txt -out client.encrypted.key 4096

# Generate Req
openssl req -new -passin pass:password -key client.encrypted.key -out client.encrypted.csr -subj $SUBJ

# Generate x509 with signed CA
openssl x509 \
        -req \
        -in client.encrypted.csr \
        -CA ca.crt \
        -CAkey ca.key \
        -days 3650 \
        -sha256 \
        -extfile ../client_cert_ext.cfg \
        -out client.encrypted.crt \
        -CAcreateserial

echo ""
echo " == Validate Server"
openssl verify -verbose -CAfile ca.crt server.crt
echo ""
echo " == Validate Client"
openssl verify -verbose -CAfile ca.crt client.crt
echo ""
echo " == Validate Client with passphrase"
openssl verify -verbose -CAfile ca.crt client.encrypted.crt
echo ""
echo " == Transform Client from PEM to RSA format"
openssl rsa -in client.key -out client_new.key -traditional && mv client_new.key client.key
