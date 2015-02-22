#!/bin/bash

VM_NAME="$1"

# Translate name to real
VM_NAME=`cat translate | grep $VM_NAME | awk '{print $2;}'`

#TODO: replace VM_NAME by IP

# Set ssh parameters
SSH_USER="root"
SSH_OPTS=' -o StrictHostKeyChecking=no -o BatchMode=yes -o UserKnownHostsFile=/dev/null -o LogLevel=quiet '

#ssh $SSH_USER@$VM_NAME $SSH_OPTS "free -m --si | grep Mem | awk '{print $3;}'"
ssh $SSH_USER@$VM_NAME $SSH_OPTS "free -m | grep Mem | awk '{print $3;}'"