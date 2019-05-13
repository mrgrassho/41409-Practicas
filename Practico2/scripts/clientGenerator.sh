#! /bin/bash

PAYLOAD="\"{'header':{'token-id':'8228588386745170394'},'functionName':'suma','parametros':{'num1':5,'num2':7},'resultado':0}\""
USER="admin"
PASS="admin"
RT_KEY="\"inputQueue\""
P=argv[0]

for (( i = 0; i < P; i++ )); do
  rabbitmqadmin -u $USER -p $PASS publish routing_key="inputQueue" payload=$PAYLOAD
done
