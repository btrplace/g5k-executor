#!/bin/bash

VM_NAME="$1"
NODE_SRC="$2"
NODE_DEST="$3"
BANDWIDTH="$4"
VIRSH_OPTS="$5"

echo "Migrate $VM_NAME from $NODE_SRC to $NODE_DEST at $BANDWIDTH mb/s (`date +%S`)"
sleep 2