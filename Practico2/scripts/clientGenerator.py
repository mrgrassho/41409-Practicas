#!/usr/bin/env python3

'''
    Genera mensajes destinados a la cola de Input.
'''

from subprocess import call
from random import randrange
import json
import time

USER = "admin"
PASS = "admin"

payload = '\'{"header":{"token-id":"8228588386745170394"},\
            "functionName":"suma",\
            "parametros":{"num1":5,"num2":7},"resultado":0}\''

def main(x=500):
    for i in range(x):
        i = randrange(12304302498231309);
        args = "rabbitmqadmin -u "+USER+" -p "+PASS+" publish  routing_key=\"inputQueue\" \
            payload=" + payload
        call(args, shell=True)
        if i % 10 == 0:
            time.sleep(1)


if __name__ == '__main__':
    main()
