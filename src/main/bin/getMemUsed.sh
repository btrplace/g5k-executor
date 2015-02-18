#!/bin/bash

VM_NAME="$1"

# Set ssh parameters
SSH_USER="root"
SSH_OPTS=' -o StrictHostKeyChecking=no -o BatchMode=yes -o UserKnownHostsFile=/dev/null -o LogLevel=quiet '

ssh $SSH_USER@$VM_NAME $SSH_OPTS "free -m --si | grep Mem | awk '{print $3;}'"