#!/usr/bin/env python3

from subprocess import call
from random import randrange

def main(x=3):
    for i in range(x):
        i = randrange(12304302498231309);
        args = "rabbitmqadmin -u admin -p admin publish routing_key=\"inputQueue\" \
            payload=\'{\"header\":{\"token-id\":\""+ str(i) +"\"},\
            \"body\":\"NEW TASK\"}"
        print(args);
        call(args, shell=True)


if __name__ == '__main__':
    main()
