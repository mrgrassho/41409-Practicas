#!/usr/bin/env python3

'''
    Genera mensajes destinados a la cola de Input.
'''

from subprocess import call
from random import randrange

payload = '\'{"header":{"token-id":"8228588386745170394"},\
            "functionName":"suma",\
            "parametros":{"num1":5,"num2":7},"resultado":0}\''

def main(x=20):
    for i in range(x):
        i = randrange(12304302498231309);
        args = "rabbitmqadmin -u admin -p admin publish routing_key=\"inputQueue\" \
            payload=" + payload
        call(args, shell=True)


if __name__ == '__main__':
    main()
