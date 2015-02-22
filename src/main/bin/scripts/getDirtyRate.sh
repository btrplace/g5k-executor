#!/bin/bash

HOST="$1"
VM_NAME="$2"
FREQUENCY="$3"
DURATION="$4"

# Translate name to real
HOST=`cat translate | grep $HOST | awk '{print $2;}'`

SSH_USER=
SSH_OPTS=' -o StrictHostKeyChecking=no -o BatchMode=yes -o UserKnownHostsFile=/dev/null -o LogLevel=quiet '

virsh -c qemu+tcp://$HOST/system qemu-monitor-command $VM_NAME '{ "execute": "get-dirty-pages", "arguments": { "file": "/tmp/dprate", "freq": $FREQUENCY, "delay": $DURATION } }'
sleep $(( ($DURATION/1000) + 1 ))
ssh $SSH_USER@$HOST $SSH_OPTS "cat /tmp/dprate"