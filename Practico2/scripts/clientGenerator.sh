#! /bin/bash

PAYLOAD="{'header':{'token-id':'8228588386745170394'},'functionName':'suma','parametros':{'num1':5,'num2':7},'resultado':0}"
USER="admin"
PASS="admin"
RTNG_KEY="'inputQueue'"
QTY=10

echo "Generating users requests..."
for (( i = 0; i < $QTY; i++ )); do
  rabbitmqadmin -u $USER -p $PASS publish routing_key=$RTNG_KEY payload=$PAYLOAD &> /dev/null
done
